#!/bin/bash

# --- Configuration ---
MINECRAFT_VERSION="1.21.5"
PURPUR_BUILD="2450"
SERVER_JAR_URL="https://api.purpurmc.org/v2/purpur/${MINECRAFT_VERSION}/${PURPUR_BUILD}/download"
SERVER_DIR="server"
JAR_NAME="purpur-${MINECRAFT_VERSION}-${PURPUR_BUILD}.jar"
SERVER_JAR_PATH="${SERVER_DIR}/${JAR_NAME}"
EULA_PATH="${SERVER_DIR}/eula.txt"
SCREEN_NAME="mmocraft_server" # Name for the screen session

# --- Java Settings ---
MEMORY_ARGS="-Xms2G -Xmx2G"

# --- Helper Functions ---
function print_info {
    echo "[INFO] $1"
}

function print_error {
    echo "[ERROR] $1" >&2
}

function print_usage {
    echo "Usage: $0 {start|stop|restart|status|console|command|setup|build|deploy}"
    echo "  start     - Builds, deploys, and starts the server in a screen session."
    echo "  stop      - Stops the server gracefully."
    echo "  restart   - Stops, rebuilds, deploys, and starts the server."
    echo "  status    - Checks if the server screen session is running."
    echo "  console   - Attaches to the interactive server console."
    echo "  command   - Sends a command to the server (e.g., '$0 command say Hello')."
    echo "  setup     - Performs the initial server setup (download, eula)."
    echo "  build     - Compiles the plugin."
    echo "  deploy    - Copies the built plugin to the server directory."
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
            print_error "Failed to download Purpur JAR."
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

function is_running {
    if screen -list | grep -q "\.${SCREEN_NAME}"; then
        return 0 # Screen session exists
    else
        return 1 # No such screen session
    fi
}

function start_server {
    if ! command -v screen &> /dev/null; then
        print_error "'screen' is not installed. Please install it to use the interactive console."
        exit 1
    fi

    if is_running; then
        print_error "Server is already running in screen session '$SCREEN_NAME'."
        exit 1
    fi

    build_plugin
    deploy_plugin

    if [ ! -f "$SERVER_JAR_PATH" ]; then
        print_info "Server JAR not found. Running setup first."
        setup_server
    fi

    print_info "Starting Minecraft server in screen session '$SCREEN_NAME'..."
    cd "$SERVER_DIR" || exit
    # Start server in a detached screen session
    screen -S "$SCREEN_NAME" -d -m java $MEMORY_ARGS -jar "$JAR_NAME" --nogui
    cd ..

    sleep 3
    if is_running; then
        print_info "Server started successfully."
        print_info "To connect to the console, run: $0 console"
        print_info "To send a command, run: $0 command <your-command>"
    else
        print_error "Server failed to start. Check for crash logs in '${SERVER_DIR}/crash-reports/' or view the screen buffer with 'screen -r ${SCREEN_NAME}'."
    fi
}

function stop_server {
    if ! is_running; then
        print_error "Server is not running."
        exit 1
    fi

    print_info "Sending 'stop' command to the server..."
    screen -S "$SCREEN_NAME" -p 0 -X stuff "stop\n"

    local count=0
    print_info "Waiting for server to shut down..."
    while is_running; do
        sleep 1
        count=$((count + 1))
        if [ $count -gt 30 ]; then
            print_error "Server did not stop after 30 seconds. You may need to kill the screen session manually: 'screen -X -S ${SCREEN_NAME} quit'"
            exit 1
        fi
    done
    print_info "Server stopped."
}

function server_status {
    if is_running; then
        print_info "Server is RUNNING in screen session '$SCREEN_NAME'."
    else
        print_info "Server is STOPPED."
    fi
}

function attach_console {
    if ! is_running; then
        print_error "Server is not running. Cannot attach to console."
        exit 1
    fi
    print_info "Attaching to server console. Press 'Ctrl+A' then 'D' to detach."
    screen -r "$SCREEN_NAME"
}

function send_command {
    if ! is_running; then
        print_error "Server is not running. Cannot send command."
        exit 1
    fi
    if [ -z "$1" ]; then
        print_error "No command provided."
        print_usage
        exit 1
    fi

    # The rest of the arguments are treated as the command
    local cmd="$*"
    print_info "Sending command: '$cmd'"
    screen -S "$SCREEN_NAME" -p 0 -X stuff "$cmd\n"
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
        attach_console
        ;;
    command)
        shift # Remove 'command' from the arguments
        send_command "$@"
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
