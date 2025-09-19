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

DEMO_PREF_DIR="${SERVER_DIR}/plugins/MMOCraft/setup"
DEMO_PREF_FILE="${DEMO_PREF_DIR}/demo-preferences.properties"

# --- Java Settings ---
MEMORY_ARGS="-Xms2G -Xmx2G"

# --- EULA Settings ---
# Set to "true" to automatically accept the Minecraft EULA.
# By doing so, you are indicating your agreement to the EULA (https://aka.ms/MinecraftEULA).
AUTO_ACCEPT_EULA="false"

# --- Helper Functions ---
function print_info {
    echo "[INFO] $1"
}

function print_error {
    echo "[ERROR] $1" >&2
}

function print_usage {
    echo "Usage: $0 {start|stop|restart|status|console|setup|build|deploy}"
    echo "  start     - Builds, deploys, and starts the server in a screen session."
    echo "  stop      - Stops the server gracefully."
    echo "  restart   - Stops, rebuilds, deploys, and starts the server."
    echo "  status    - Checks if the server screen session is running."
    echo "  console   - Attaches to the interactive server console."
    echo "  setup     - Performs the initial server setup (download, eula)."
    echo "  build     - Compiles the plugin."
    echo "  deploy    - Copies the built plugin to the server directory."
}

function sed_inplace {
    local expression="$1"
    local file="$2"
    if [[ "${OSTYPE}" == "darwin"* ]]; then
        sed -i '' "$expression" "$file"
    else
        sed -i "$expression" "$file"
    fi
}

function get_server_port {
    local properties_file="${SERVER_DIR}/server.properties"
    if [ -f "$properties_file" ]; then
        # Use grep and cut to extract the port number
        local port
        port=$(grep -E '^server-port=' "$properties_file" | cut -d'=' -f2)
        # Check if port is a number, otherwise return default
        if [[ "$port" =~ ^[0-9]+$ ]]; then
            echo "$port"
        else
            echo "25565"
        fi
    else
        # Fallback to default if server.properties doesn't exist
        echo "25565"
    fi
}

function cleanup_lingering_processes {
    print_info "Checking for lingering server processes..."
    # Check if lsof command exists, as it's the most reliable way to find a process by port
    if ! command -v lsof &> /dev/null; then
        print_info "'lsof' is not available. Skipping check for lingering processes by port."
        # As a fallback, we can still try to kill any stray screen sessions.
        if is_running; then
            print_info "Terminating stray screen session '${SCREEN_NAME}'..."
            screen -X -S "${SCREEN_NAME}" quit
        fi
        return
    fi

    local server_port
    server_port=$(get_server_port)
    # The -t flag gives just the PID, making it easy to script with.
    # The -i :<port> flag finds processes using that TCP/UDP port.
    local pid
    pid=$(lsof -t -i :"$server_port")

    if [ -n "$pid" ]; then
        print_info "Found lingering process on port $server_port with PID $pid. Terminating..."
        # Try to terminate gracefully first with SIGTERM
        if kill "$pid"; then
            local count=0
            # Wait up to 10 seconds for the process to die
            while kill -0 "$pid" 2>/dev/null; do
                sleep 1
                count=$((count + 1))
                if [ $count -gt 10 ]; then
                    print_error "Process $pid did not terminate after 10 seconds. Forcing shutdown (SIGKILL)..."
                    kill -9 "$pid"
                    sleep 2 # Give OS time to reap the process
                    break
                fi
            done
            print_info "Process terminated."
        else
            print_error "Failed to send termination signal to process $pid. It may already be gone."
        fi
    else
        print_info "No lingering process found on port $server_port."
    fi

    # Also make sure the screen session is gone, in case it's alive but the process isn't listening.
    if is_running; then
        print_info "Terminating stray screen session '${SCREEN_NAME}'..."
        screen -X -S "${SCREEN_NAME}" quit
    fi
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

    mkdir -p "${SERVER_DIR}/plugins"

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

    # Check if EULA needs to be handled
    if [ ! -f "$EULA_PATH" ] || ! grep -q "eula=true" "$EULA_PATH"; then
        # Run server once to generate files if eula.txt doesn't exist
        if [ ! -f "$EULA_PATH" ]; then
            print_info "First-time setup: Generating server files..."
            (cd "$SERVER_DIR" && java -jar "$JAR_NAME" --initSettings)
            print_info "Server files generated."
        fi

        if [ "$AUTO_ACCEPT_EULA" = "true" ]; then
            print_info "Automatically accepting EULA..."
            echo "eula=true" > "$EULA_PATH"
            print_info "EULA has been accepted."
        else
            print_info "You need to agree to the Minecraft EULA."
            print_info "Please read the EULA at: https://aka.ms/MinecraftEULA"
            # Show the official text from the generated eula.txt
            if [ -f "$EULA_PATH" ]; then
                echo "---"
                # The eula.txt file has comments we can show the user
                grep '#' "$EULA_PATH"
                echo "---"
            fi

            read -p "Do you agree to the EULA? (y/n) " -n 1 -r
            echo # Move to a new line
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                print_info "Accepting EULA..."
                # Use sed to change eula=false/eula=true (portable syntax for Linux and macOS)
                if ! sed_inplace 's/eula=false/eula=true/' "$EULA_PATH"; then
                    print_error "Failed to update eula.txt. Please edit it manually. Aborting."
                    exit 1
                fi
                print_info "EULA has been accepted."
            else
                print_error "You must agree to the EULA to run the server. Aborting."
                exit 1
            fi
        fi
    else
        print_info "EULA already accepted."
    fi

    configure_demo_preferences
    print_info "Setup complete."
}

function is_running {
    if screen -S "$SCREEN_NAME" -Q select . &>/dev/null; then
        return 0
    fi

    if screen -ls | grep -qE "\.${SCREEN_NAME}[[:space:]]" 2>/dev/null; then
        return 0
    fi

    if pgrep -f "$JAR_NAME" >/dev/null 2>&1; then
        return 0
    fi

    return 1
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

    cleanup_lingering_processes

    build_plugin
    deploy_plugin

    if [ ! -f "$SERVER_JAR_PATH" ]; then
        print_info "Server JAR not found. Running setup first."
        setup_server
    fi

    print_info "Starting Minecraft server in screen session '$SCREEN_NAME'..."
    cd "$SERVER_DIR" || exit
    # Start server in a detached screen session
    # The -L flag enables logging to a file, typically named "screen.0", "screen.1", etc.
    screen -L -S "$SCREEN_NAME" -d -m java $MEMORY_ARGS -jar "$JAR_NAME" --nogui
    cd ..

    sleep 3
    if ! is_running; then
        print_error "Server failed to start. Check for crash logs in '${SERVER_DIR}/crash-reports/' or the screen log (e.g., '${SERVER_DIR}/screen.0'). You can also view the buffer with 'screen -r ${SCREEN_NAME}'."
        return 1
    fi

    if wait_for_server_ready; then
        local port
        port=$(get_server_port)
        print_info "Server started successfully."
        print_info "Minecraft server is ready on port ${port}."
        print_info "You can join using: localhost:${port} (Minecraft ${MINECRAFT_VERSION})."
        print_info "To connect to the console, run: $0 console"
    else
        print_error "Server failed to report ready status. Review the logs in '${SERVER_DIR}/logs/latest.log'."
        return 1
    fi
}

function stop_server {
    print_info "Initiating server shutdown sequence..."
    if is_running; then
        print_info "Attempting graceful shutdown via screen session..."
        screen -S "$SCREEN_NAME" -p 0 -X stuff "stop\n"

        local count=0
        print_info "Waiting up to 30 seconds for server to stop..."
        while is_running; do
            sleep 1
            count=$((count + 1))
            if [ $count -gt 30 ]; then
                print_error "Server did not stop gracefully after 30 seconds. Proceeding to force cleanup."
                break # Exit loop and proceed to cleanup
            fi
        done
        print_info "Graceful shutdown complete or timed out."
    else
        print_info "No running screen session found."
    fi

    # Forcefully clean up any processes that might be left over.
    cleanup_lingering_processes

    print_info "Server stop sequence complete."
}

function wait_for_server_ready {
    local log_file="${SERVER_DIR}/logs/latest.log"
    local timeout=180
    local elapsed=0

    print_info "Waiting for server startup to complete (timeout ${timeout}s)..."

    while [ $elapsed -lt $timeout ]; do
        if ! is_running; then
            print_error "Server process is not running while waiting for startup confirmation."
            return 1
        fi

        if [ -f "$log_file" ]; then
            if grep -q "Done (" "$log_file"; then
                print_info "Server reported startup completion."
                return 0
            fi
        fi

        sleep 3
        elapsed=$((elapsed + 3))
    done

    print_error "Timed out waiting for server startup confirmation after ${timeout}s."
    return 1
}

function configure_demo_preferences {
    mkdir -p "$DEMO_PREF_DIR"

    local existing_preference=""
    if [ -f "$DEMO_PREF_FILE" ]; then
        existing_preference=$(grep -E '^enable-demo=' "$DEMO_PREF_FILE" | tail -n 1 | cut -d'=' -f2 | tr '[:upper:]' '[:lower:]')
        if [ "$existing_preference" = "true" ]; then
            print_info "Current demo content preference: ENABLED"
        elif [ "$existing_preference" = "false" ]; then
            print_info "Current demo content preference: DISABLED"
        fi
    fi

    if [ ! -t 0 ]; then
        local default_value="true"
        if [ -n "$existing_preference" ]; then
            default_value="$existing_preference"
        fi
        save_demo_preference "$default_value"
        local pretty_pref="ENABLED"
        if [ "$default_value" = "false" ]; then
            pretty_pref="DISABLED"
        fi
        print_info "Non-interactive shell detected. Demo content preference set to ${pretty_pref}."
        return
    fi

    local default_choice="y"
    if [ "$existing_preference" = "false" ]; then
        default_choice="n"
    fi

    local prompt="Enable bundled MMOCraft demo content? [Y/n]: "
    local answer
    while true; do
        read -r -p "$prompt" answer
        if [ -z "$answer" ]; then
            answer="$default_choice"
        fi
        case "$answer" in
            [Yy]* )
                save_demo_preference "true"
                print_info "Demo content will be ENABLED on next server start."
                break
                ;;
            [Nn]* )
                save_demo_preference "false"
                print_info "Demo content will be DISABLED on next server start."
                break
                ;;
            * )
                echo "Please answer 'y' or 'n'."
                ;;
        esac
    done
}

function save_demo_preference {
    local value="$1"
    {
        echo "# Generated by server.sh on $(date)"
        echo "enable-demo=${value}"
    } > "$DEMO_PREF_FILE"
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

# --- Main Logic ---
exit_code=0
case "$1" in
    start)
        start_server || exit_code=$?
        ;;
    stop)
        stop_server || exit_code=$?
        ;;
    restart)
        print_info "Executing server restart..."
        stop_server || exit_code=$?
        if [ $exit_code -eq 0 ]; then
            print_info "Waiting a few seconds before starting again..."
            sleep 3
            start_server || exit_code=$?
        fi
        ;;
    status)
        server_status || exit_code=$?
        ;;
    console)
        attach_console || exit_code=$?
        ;;
    setup)
        setup_server || exit_code=$?
        ;;
    build)
        build_plugin || exit_code=$?
        ;;
    deploy)
        deploy_plugin || exit_code=$?
        ;;
    *)
        print_usage
        exit 1
        ;;
esac

exit $exit_code
