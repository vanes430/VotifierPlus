package config

import (
	"crypto/rand"
	"encoding/hex"
	"os"

	"gopkg.in/yaml.v3"
)

type Config struct {
	Host           string                 `yaml:"Host"`
	Port           int                    `yaml:"Port"`
	Debug          bool                   `yaml:"Debug"`
	TokenSupport   bool                   `yaml:"TokenSupport"`
	Tokens         map[string]string      `yaml:"Tokens"`
	Forwarding     map[string]ForwardNode `yaml:"Forwarding"`
}

type ForwardNode struct {
	Address  string `yaml:"Address"`
	Key      string `yaml:"Key"`
	Token    string `yaml:"Token"`
	UseToken bool   `yaml:"UseToken"`
	Enabled  bool   `yaml:"Enabled"`
}

func GenerateToken() string {
	b := make([]byte, 8)
	if _, err := rand.Read(b); err != nil {
		return "default_token_123"
	}
	return hex.EncodeToString(b)
}

func LoadConfig(path string) (*Config, error) {
	// Default values
	cfg := &Config{
		Host:         "0.0.0.0",
		Port:         8192,
		Debug:        false,
		TokenSupport: false, // Default DISABLED
		Tokens:       make(map[string]string),
		Forwarding:   make(map[string]ForwardNode),
	}

	// Check if file exists
	if _, err := os.Stat(path); os.IsNotExist(err) {
		return cfg, CreateDefaultConfig(path, cfg)
	}

	// Read file
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}

	// Unmarshal
	err = yaml.Unmarshal(data, cfg)
	if err != nil {
		return nil, err
	}

	return cfg, nil
}

func CreateDefaultConfig(path string, cfg *Config) error {
	// Generate a secure token for local server identity
	cfg.Tokens["default"] = GenerateToken()

	// Add an example forwarding node with empty token/key
	cfg.Forwarding["example_server"] = ForwardNode{
		Address:  "127.0.0.1:8193",
		Enabled:  false,
		UseToken: false,
		Token:    "", // User filling this manually
		Key:      "", // User filling this manually
	}
	
data, err := yaml.Marshal(cfg)
	if err != nil {
		return err
	}
	
	header := "# VotifierPlus Golang Configuration\n" +
		"# Host: 0.0.0.0 to listen on all interfaces\n" +
		"# Port: Default Votifier port is 8192\n\n"
	
	finalData := append([]byte(header), data...)
	
	return os.WriteFile(path, finalData, 0644)
}