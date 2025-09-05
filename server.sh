#!/bin/bash

# --- Configuration ---
MINECRAFT_VERSION="1.21.5"
PURPUR_BUILD="2450" # The latest build for 1.21.5 as of writing
SERVER_JAR_URL="https://api.purpurmc.org/v2/purpur/${MINECRAFT_VERSION}/${PURPUR_BUILD}/download"
SERVER_DIR="server"
JAR_NAME="purpur-${MINECRAFT_VERSION}-${PURPUR_BUILD}.jar"
SERVER_JAR_PATH="${SERVER_DIR}/${JAR_NAME}"
EULA_PATH="${SERVER_DIR}/eula.txt"
PID_FILE="${SERVER_DIR}/server.pid"
LOG_FILE="${SERVER_DIR}/logs/latest.log"

# --- Java Settings ---
MEMORY_ARGS="-Xms2G -Xmx2G" # 2GB initial and max heap size

# --- Helper Functions ---
function print_info {
    echo "[INFO] $1"
}

function print_error {
    echo "[ERROR] $1" >&2
}

function print_usage {
    echo "Usage: $0 {start|stop|restart|status|console|setup|build|deploy}"
    echo "  start   - Builds, deploys, and starts the server."
    echo "  stop    - Stops the server."
    echo "  restart - Stops, rebuilds, deploys, and starts the server."
    echo "  status  - Checks if the server is running."
    echo "  console - Attaches to the server console log."
    echo "  setup   - Performs the initial server setup (download, eula)."
    echo "  build   - Compiles the plugin."
    echo "  deploy  - Copies the built plugin to the server directory."
}

# --- Core Functions ---

function build_plugin {
    print_info "Building the MMOCraft plugin..."
    if ! ./gradlew clean build; then
        print_error "Plugin build failed. Please check the Gradle output. Aborting."
        exit 1
    fi
    print_info "Plugin built successfully."
}

function deploy_plugin {
    print_info "Deploying plugin to server..."
    # Find the plugin jar, ignoring sources and javadoc jars
    local plugin_jar
    plugin_jar=$(find build/libs -type f -name "*.jar" ! -name "*-sources.jar" ! -name "*-javadoc.jar" | head -n 1)

    if [ -z "$plugin_jar" ]; then
        print_error "Could not find the plugin JAR file in build/libs/. Aborting."
        exit 1
    fi

    local plugin_name
    plugin_name=$(basename "$plugin_jar")
    print_info "Found plugin: $plugin_name"

    if ! cp "$plugin_jar" "${SERVER_DIR}/plugins/"; then
        print_error "Failed to copy plugin to ${SERVER_DIR}/plugins/. Aborting."
        exit 1
    fi

    print_info "Plugin successfully deployed to the server."
}

function setup_server {
    print_info "Starting server setup..."
    mkdir -p "${SERVER_DIR}"

    if [ ! -f "$SERVER_JAR_PATH" ]; then
        print_info "Purpur JAR not found. Downloading..."
        if ! curl -L -o "$SERVER_JAR_PATH" "$SERVER_JAR_URL"; then
            print_error "Failed to download Purpur JAR. Please check the URL or your internet connection."
            exit 1
        fi
        print_info "Download complete."
    else
        print_info "Purpur JAR already exists."
    fi

    if [ ! -f "$EULA_PATH" ]; then
        print_info "EULA not accepted. Creating eula.txt..."
        echo "eula=true" > "$EULA_PATH"
        print_info "EULA has been accepted."
    else
        print_info "eula.txt already exists."
    fi
    print_info "Setup complete."
}

function start_server {
    if is_running; then
        print_error "Server is already running."
        exit 1
    fi

    # Build and deploy the plugin first
    build_plugin
    deploy_plugin

    if [ ! -f "$SERVER_JAR_PATH" ]; then
        print_info "Server JAR not found. Running setup first."
        setup_server
    fi

    cd "$SERVER_DIR" || exit
    print_info "Starting Minecraft server..."
    nohup java $MEMORY_ARGS -jar "$JAR_NAME" --nogui > /dev/null 2>&1 &
    echo $! > "server.pid"
    cd ..

    sleep 5 # Give server time to start up a bit
    if is_running; then
        print_info "Server started successfully with PID $(cat $PID_FILE)."
        print_info "To view the console, run: $0 console"
    else
        print_error "Server failed to start. Check logs in ${SERVER_DIR}/logs/"
    fi
}

function stop_server {
    if ! is_running; then
        print_error "Server is not running."
        exit 1
    fi

    local pid
    pid=$(cat "$PID_FILE")
    print_info "Stopping server with PID $pid..."
    kill "$pid"

    # Wait for server to stop
    local count=0
    while is_running; do
        sleep 1
        count=$((count + 1))
        if [ $count -gt 15 ]; then
            print_error "Server did not stop gracefully after 15 seconds. Forcing shutdown..."
            kill -9 "$pid"
            break
        fi
    done

    rm -f "$PID_FILE"
    print_info "Server stopped."
}

function is_running {
    if [ -f "$PID_FILE" ]; then
        local pid
        pid=$(cat "$PID_FILE")
        if ps -p "$pid" > /dev/null; then
            return 0 # Process is running
        else
            # PID file exists but process is not running, cleanup
            rm -f "$PID_FILE"
            return 1
        fi
    fi
    return 1 # Not running
}

function server_status {
    if is_running; then
        print_info "Server is RUNNING with PID $(cat $PID_FILE)."
    else
        print_info "Server is STOPPED."
    fi
}

function view_console {
    if [ -f "$LOG_FILE" ]; then
        tail -f "$LOG_FILE"
    else
        print_error "Log file not found. Is the server running for the first time?"
    fi
}


# --- Main Logic ---
case "$1" in
    start)
        start_server
        ;;
    stop)
        stop_server
        ;;
    restart)
        if is_running; then
            stop_server
        fi
        start_server
        ;;
    status)
        server_status
        ;;
    console)
        view_console
        ;;
    setup)
        setup_server
        ;;
    build)
        build_plugin
        ;;
    deploy)
        deploy_plugin
        ;;
    *)
        print_usage
        exit 1
        ;;
esac

exit 0
