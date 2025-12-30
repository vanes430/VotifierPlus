package main

import (
	"bufio"
	"fmt"
	"log"
	"os"
	"runtime"
	"strings"
	"time"

	"github.com/vanes430/VotifierPlus/standalone-golang/config"
	"github.com/vanes430/VotifierPlus/standalone-golang/crypto"
	"github.com/vanes430/VotifierPlus/standalone-golang/server"
)

var (
	banner = []string{
		"                                                         ",
		"██╗   ██╗ ██████╗ ████████╗██╗███████╗██╗███████╗██████╗ ",
		"██║   ██║██╔═══██╗╚══██╔══╝██║██╔════╝██║██╔════╝██╔══██╗",
		"██║   ██║██║   ██║   ██║   ██║█████╗  ██║█████╗  ██████╔╝",
		"╚██╗ ██╔╝██║   ██║   ██║   ██║██╔══╝  ██║██╔══╝  ██╔══██╗",
		" ╚████╔╝ ╚██████╔╝   ██║   ██║██║     ██║███████╗██║  ██║",
		"  ╚═══╝   ╚═════╝    ╚═╝   ╚═╝╚═╝     ╚═╝╚══════╝╚═╝  ╚═╝",
		"                                                         ",
		"   > Fork maintained by vanes430",
		"   > GitHub: https://github.com/vanes430/VotifierPlus",
		"   > VotifierPlus-Golang v1.0.0",
		"                                                         ",
	}
	startTime  time.Time
	voteServer *server.VoteServer
)

func printBanner() {
	for _, line := range banner {
		fmt.Println(line)
	}
}

func initSystem() {
	// Stop existing server if reloading
	if voteServer != nil {
		voteServer.Stop()
	}

	var err error
	// 1. Load Config
	cfg, err := config.LoadConfig("config.yml")
	if err != nil {
		log.Printf("Error loading config: %v", err)
		return
	}
	
	// 2. Load/Generate Keys
	keys, err := crypto.LoadOrGenerateKeys("rsa")
	if err != nil {
		log.Printf("Error loading RSA keys: %v", err)
		return
	}

	// 3. Start Server
	// Pass tokens from config to server
	voteServer = server.NewVoteServer(cfg.Host, cfg.Port, keys, cfg.Tokens, cfg.Debug)
	err = voteServer.Start()
	if err != nil {
		log.Printf("Failed to start listener: %v", err)
		return
	}

	log.Printf("RSA Keys loaded (%d bits).", keys.N.BitLen())
	
	// Feature: One-line Public Key
	pubOneLine, _ := crypto.PublicKeyToSingleLine(&keys.PublicKey)
	log.Println("----------------------------------------------------------------")
	log.Println("PUBLIC KEY (Use this for your server list):")
	log.Println(pubOneLine)
	log.Println("----------------------------------------------------------------")

	// Log active tokens
	if cfg.TokenSupport {
		log.Println("V2 Token Support: ENABLED")
		log.Println("----------------------------------------------------------------")
		log.Println("TOKENS:")
		for service, token := range cfg.Tokens {
			log.Printf(" - %s: %s", service, token)
		}
		log.Println("----------------------------------------------------------------")
	} else {
		log.Println("V2 Token Support: DISABLED (Enable 'TokenSupport' in config.yml to use V2 protocol)")
	}
}

func main() {
	startTime = time.Now()
	printBanner()
	log.Println("Initializing VotifierPlus (Golang)...")

	initSystem()

	// Start Command Handler
	go handleCommands()

	// Keep main goroutine alive
	select {} 
}

func handleCommands() {
	scanner := bufio.NewScanner(os.Stdin)
	for scanner.Scan() {
		input := strings.TrimSpace(scanner.Text())
		cmd := strings.ToLower(input)

		switch cmd {
		case "stop", "exit", "quit":
			log.Println("Stopping VotifierPlus...")
			if voteServer != nil {
				voteServer.Stop()
			}
			os.Exit(0)
		
		case "reload":
			log.Println("Reloading configuration...")
			initSystem()
			log.Println("Reload complete.")

		case "uptime":
			dur := time.Since(startTime)
			log.Printf("Uptime: %s", dur.Round(time.Second))

		case "status":
			var m runtime.MemStats
			runtime.ReadMemStats(&m)
			log.Println("--- System Status ---")
			log.Printf("Goroutines: %d", runtime.NumGoroutine())
			log.Printf("Memory Alloc: %d KB", m.Alloc/1024)
			log.Printf("Total Alloc: %d KB", m.TotalAlloc/1024)
			log.Println("---------------------")

		case "help":
			log.Println("Available commands: stop, reload, uptime, status, help")

		default:
			if cmd != "" {
				log.Printf("Unknown command: '%s'. Type 'help' for list.", cmd)
			}
		}
	}
}