package server

import (
	"bufio"
	"bytes"
	"crypto/hmac"
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha256"
	"encoding/base64"
	"encoding/binary"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net"
	"strings"
	"time"
)

type VoteServer struct {
	Host       string
	Port       int
	PrivateKey *rsa.PrivateKey
	Tokens     map[string]string
	Debug      bool
	listener   net.Listener
	running    bool
}

func NewVoteServer(host string, port int, pk *rsa.PrivateKey, tokens map[string]string, debug bool) *VoteServer {
	return &VoteServer{
		Host:       host,
		Port:       port,
		PrivateKey: pk,
		Tokens:     tokens,
		Debug:      debug,
	}
}

func (s *VoteServer) debugLog(format string, v ...interface{}) {
	if s.Debug {
		log.Printf("[DEBUG] "+format, v...)
	}
}

func (s *VoteServer) Start() error {
	addr := fmt.Sprintf("%s:%d", s.Host, s.Port)
	ln, err := net.Listen("tcp", addr)
	if err != nil {
		return err
	}
	s.listener = ln
	s.running = true
	
	log.Printf("[Server] Listening on %s", addr)

	go s.acceptLoop()
	return nil
}

func (s *VoteServer) Stop() {
	s.running = false
	if s.listener != nil {
		s.listener.Close()
	}
}

func (s *VoteServer) acceptLoop() {
	for s.running {
		conn, err := s.listener.Accept()
		if err != nil {
			if s.running {
				log.Printf("[Server] Accept error: %v", err)
			}
			continue
		}
		s.debugLog("Accepted connection from %s", conn.RemoteAddr())
		go s.handleConnection(conn)
	}
}

// Generate a 32-char random challenge string
func generateChallenge() string {
	b := make([]byte, 16)
	rand.Read(b)
	return hex.EncodeToString(b)
}

func (s *VoteServer) handleConnection(conn net.Conn) {
	defer func() {
		s.debugLog("Closing connection from %s", conn.RemoteAddr())
		conn.Close()
	}()
	conn.SetDeadline(time.Now().Add(10 * time.Second))

	challenge := generateChallenge()
	
	// Determine if we support V2 (have tokens)
	useV2 := len(s.Tokens) > 0
	s.debugLog("V2 Support: %v, Challenge: %s", useV2, challenge)

	// 1. Send Handshake
	var handshake string
	if useV2 {
		handshake = fmt.Sprintf("VOTIFIER 2 %s\n", challenge)
	} else {
		handshake = "VOTIFIER 1\n"
	}

	_, err := conn.Write([]byte(handshake))
	if err != nil {
		log.Printf("[Vote] Handshake error: %v", err)
		return
	}

	// 2. Peek/Read Header to determine Protocol
	// We read the first 2 bytes to check for magic
	header := make([]byte, 2)
	_, err = io.ReadFull(conn, header)
	if err != nil {
		log.Printf("[Vote] Read header error: %v", err)
		return
	}

	// Protocol Detection
	isV2 := false
	var payloadBytes []byte

	// Check for NuVotifier binary header "s:" (0x73 0x3A)
	if header[0] == 0x73 && header[1] == 0x3A {
		isV2 = true
		s.debugLog("Protocol V2 (Binary) detected")
		// Read length (2 bytes, big endian)
		lenBytes := make([]byte, 2)
		if _, err := io.ReadFull(conn, lenBytes); err != nil {
			log.Printf("[Vote V2] Failed to read length: %v", err)
			return
		}
		length := binary.BigEndian.Uint16(lenBytes)
		s.debugLog("V2 Payload length: %d", length)
		
		payloadBytes = make([]byte, length)
		if _, err := io.ReadFull(conn, payloadBytes); err != nil {
			log.Printf("[Vote V2] Failed to read payload: %v", err)
			return
		}

	} else if header[0] == '{' {
		// Pure JSON V2
		isV2 = true
		s.debugLog("Protocol V2 (JSON) detected")
		// We read 2 bytes '{' and something else. We need to keep reading until we get a valid JSON object or EOF?
		// Usually just read until newline or EOF.
		// Let's use a bufio scanner from here, but prepending the header we already read.
		scanner := bufio.NewReader(io.MultiReader(bytes.NewReader(header), conn))
		line, _, err := scanner.ReadLine()
		if err != nil {
			log.Printf("[Vote V2] JSON read error: %v", err)
			return
		}
		payloadBytes = line

	} else {
		// Assume V1 (RSA Block)
		s.debugLog("Protocol V1 detected")
		// We already read 2 bytes of the 256 byte block. Need to read the rest.
		block := make([]byte, 256)
		copy(block, header)
		
		// Read remaining 254 bytes
		if _, err := io.ReadFull(conn, block[2:]); err != nil {
			log.Printf("[Vote V1] Failed to read full block: %v", err)
			return
		}
		
		s.handleV1(block)
		return
	}

	if isV2 {
		s.debugLog("Processing V2 payload: %s", string(payloadBytes))
		if !useV2 {
			log.Printf("[Vote] Received V2 vote but TokenSupport is disabled/no tokens configured.")
			return
		}
		s.handleV2(payloadBytes, challenge, conn)
	}
}

func (s *VoteServer) handleV1(block []byte) {
	// Decrypt
	decrypted, err := rsa.DecryptPKCS1v15(rand.Reader, s.PrivateKey, block)
	if err != nil {
		log.Printf("[Vote V1] Decryption failed: %v", err)
		return
	}

	data := string(decrypted)
	parts := strings.Split(data, "\n")

	if len(parts) < 5 || parts[0] != "VOTE" {
		log.Printf("[Vote V1] Invalid packet format")
		return
	}

	log.Printf("[V1] Received vote from %s (Service: %s, IP: %s)", parts[2], parts[1], parts[3])
}

type V2Outer struct {
	Payload   string `json:"payload"`
	Signature string `json:"signature"`
}

type V2Inner struct {
	ServiceName string      `json:"serviceName"`
	Username    string      `json:"username"`
	Address     string      `json:"address"`
	TimeStamp   interface{} `json:"timestamp"`
	Challenge   string      `json:"challenge"`
}

func (s *VoteServer) handleV2(data []byte, expectedChallenge string, conn net.Conn) {
	var outer V2Outer
	if err := json.Unmarshal(data, &outer); err != nil {
		log.Printf("[Vote V2] Invalid outer JSON: %v", err)
		return
	}

	var inner V2Inner
	if err := json.Unmarshal([]byte(outer.Payload), &inner); err != nil {
		log.Printf("[Vote V2] Invalid inner JSON: %v", err)
		return
	}

	// 1. Find Token
	token, ok := s.Tokens[inner.ServiceName]
	if !ok {
		token, ok = s.Tokens["default"]
		if !ok {
			log.Printf("[Vote V2] Unknown service '%s' and no default token.", inner.ServiceName)
			return
		}
	}

	// 2. Verify Signature
	sigBytes, err := base64.StdEncoding.DecodeString(outer.Signature)
	if err != nil {
		log.Printf("[Vote V2] Invalid base64 signature")
		return
	}

	mac := hmac.New(sha256.New, []byte(token))
	mac.Write([]byte(outer.Payload))
	expectedSig := mac.Sum(nil)

	if !hmac.Equal(sigBytes, expectedSig) {
		log.Printf("[Vote V2] Signature mismatch! Possible bad token.")
		return
	}

	// 3. Verify Challenge
	if inner.Challenge != expectedChallenge {
		log.Printf("[Vote V2] Challenge mismatch! Expected %s, got %s", expectedChallenge, inner.Challenge)
		return
	}

	log.Printf("[V2] Received vote from %s (Service: %s, IP: %s)", inner.Username, inner.ServiceName, inner.Address)

	// 4. Send Response (Java impl sends {"status":"ok"}\r\n)
	response := `{"status":"ok"}` + "\r\n"
	conn.Write([]byte(response))
}