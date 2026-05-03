#!/usr/bin/env bash
# INVOKE — Single-command startup script
# Starts all 3 processes: Ollama, Whisper server, Tauri dev server
# Usage: ./start.sh [--dev|--build]

set -e

echo "🔮 INVOKE Startup"
echo "=================="

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

check_command() {
    if ! command -v "$1" &> /dev/null; then
        echo -e "${RED}❌ $1 not found. Please install it first.${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ $1 found${NC}"
}

# Check prerequisites
echo ""
echo "Checking prerequisites..."
check_command "cargo"
check_command "node"
check_command "npm"
check_command "ollama"
check_command "python3"

MODE="${1:---dev}"

# 1. Start Ollama (if not running)
echo ""
echo "📋 Step 1: Starting Ollama..."
if curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Ollama already running${NC}"
else
    echo "Starting Ollama server..."
    ollama serve &
    OLLAMA_PID=$!
    sleep 3
    if curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Ollama started (PID: $OLLAMA_PID)${NC}"
    else
        echo -e "${RED}❌ Ollama failed to start${NC}"
        exit 1
    fi
fi

# Pull model if not present
echo "Ensuring Qwen 3 0.6B model is available..."
if ollama list 2>/dev/null | grep -q "qwen3:0.6b"; then
    echo -e "${GREEN}✅ qwen3:0.6b model available${NC}"
else
    echo -e "${YELLOW}⏳ Pulling qwen3:0.6b (first time only)...${NC}"
    ollama pull qwen3:0.6b
    echo -e "${GREEN}✅ Model pulled${NC}"
fi

# 2. Start Whisper server
echo ""
echo "📋 Step 2: Starting Whisper server..."
if curl -s http://127.0.0.1:8394/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Whisper server already running${NC}"
else
    # Check Python dependencies
    python3 -c "import faster_whisper, flask" 2>/dev/null || {
        echo -e "${YELLOW}⏳ Installing Python dependencies...${NC}"
        pip3 install faster-whisper flask
    }
    
    echo "Starting Whisper server on :8394..."
    python3 scripts/whisper_server.py &
    WHISPER_PID=$!
    sleep 5
    
    if curl -s http://127.0.0.1:8394/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Whisper server started (PID: $WHISPER_PID)${NC}"
    else
        echo -e "${RED}❌ Whisper server failed to start${NC}"
        exit 1
    fi
fi

# 3. Install npm dependencies
echo ""
echo "📋 Step 3: Installing frontend dependencies..."
if [ ! -d "node_modules" ]; then
    npm install
    echo -e "${GREEN}✅ Dependencies installed${NC}"
else
    echo -e "${GREEN}✅ Dependencies already installed${NC}"
fi

# 4. Start Tauri
echo ""
echo "📋 Step 4: Starting INVOKE..."
echo ""

case "$MODE" in
    --build)
        echo "Building for production..."
        cargo tauri build
        ;;
    --dev|*)
        echo "Starting dev server..."
        echo ""
        echo -e "${YELLOW}🔮 INVOKE is ready!${NC}"
        echo "   Press Ctrl+C to stop all services"
        echo ""
        cargo tauri dev
        ;;
esac

# Cleanup on exit
trap 'echo ""; echo "🛑 Stopping INVOKE..."; kill $(jobs -p) 2>/dev/null; exit 0' INT TERM
