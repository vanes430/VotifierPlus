package crypto

import (
	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"encoding/pem"
	"errors"
	"os"
	"path/filepath"
	"strings"
)

func LoadOrGenerateKeys(dir string) (*rsa.PrivateKey, error) {
	privPath := filepath.Join(dir, "private.key")
	pubPath := filepath.Join(dir, "public.key")

	// Try load
	if _, err := os.Stat(privPath); err == nil {
		return loadPrivateKey(privPath)
	}

	// Generate
	return generateAndSaveKeys(dir, privPath, pubPath)
}

func loadPrivateKey(path string) (*rsa.PrivateKey, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}

	block, _ := pem.Decode(data)
	if block == nil {
		return nil, errors.New("failed to decode PEM block containing private key")
	}

	return x509.ParsePKCS1PrivateKey(block.Bytes)
}

func generateAndSaveKeys(dir, privPath, pubPath string) (*rsa.PrivateKey, error) {
	if _, err := os.Stat(dir); os.IsNotExist(err) {
		os.MkdirAll(dir, 0755)
	}

	// Generate 2048-bit key
	priv, err := rsa.GenerateKey(rand.Reader, 2048)
	if err != nil {
		return nil, err
	}

	// Save Private
	privBytes := x509.MarshalPKCS1PrivateKey(priv)
	privBlock := &pem.Block{
		Type:  "RSA PRIVATE KEY",
		Bytes: privBytes,
	}
	
fPriv, err := os.Create(privPath)
	if err != nil {
		return nil, err
	}
	defer fPriv.Close()
	pem.Encode(fPriv, privBlock)

	// Save Public
	pubBytes, err := x509.MarshalPKIXPublicKey(&priv.PublicKey)
	if err != nil {
		return nil, err
	}
	pubBlock := &pem.Block{
		Type:  "PUBLIC KEY",
		Bytes: pubBytes,
	}

	fPub, err := os.Create(pubPath)
	if err != nil {
		return nil, err
	}
	defer fPub.Close()
	pem.Encode(fPub, pubBlock)

	return priv, nil
}

// PublicKeyToSingleLine converts the public key to a single line string (for server lists)
func PublicKeyToSingleLine(key *rsa.PublicKey) (string, error) {
	pubBytes, err := x509.MarshalPKIXPublicKey(key)
	if err != nil {
		return "", err
	}
	pubBlock := &pem.Block{
		Type:  "PUBLIC KEY",
		Bytes: pubBytes,
	}
	pemBytes := pem.EncodeToMemory(pubBlock)
	
	// Remove headers and newlines
	s := string(pemBytes)
	s = strings.ReplaceAll(s, "-----BEGIN PUBLIC KEY-----", "")
	s = strings.ReplaceAll(s, "-----END PUBLIC KEY-----", "")
	s = strings.ReplaceAll(s, "\n", "")
	s = strings.ReplaceAll(s, "\r", "")
	return strings.TrimSpace(s), nil
}