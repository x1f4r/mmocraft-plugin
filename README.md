# MMOCraft-Plugin

MMOCraft-Plugin is a Minecraft server plugin designed to introduce MMO-like features into the game.

## Prerequisites

- Java 21 JDK

## Getting Started

This repository is structured for easy setup and execution. The entire process is managed by the `server.sh` script.

1.  **Clone the repository:**
    ```sh
    git clone <repository-url>
    cd mmocraft-plugin
    ```

2.  **Run the server:**
    ```sh
    ./server.sh start
    ```
    This single command will automatically:
    - Compile the latest plugin code.
    - Copy the plugin JAR to the server directory.
    - Download the Purpur server if it's not already present.
    - Accept the EULA.
    - Start the Minecraft server.

## Management Script

All common actions are handled by the `server.sh` script.

### Available Commands

-   `./server.sh start`: Builds, deploys, and starts the server.
-   `./server.sh stop`: Stops the server gracefully.
-   `./server.sh restart`: Stops, rebuilds, deploys, and restarts the server.
-   `./server.sh console`: Attach to the live server console to view logs.
-   `./server.sh status`: Check if the server is currently running.
-   `./server.sh build`: Compiles the plugin without starting the server.
-   `./server.sh deploy`: Copies the already-built plugin to the server directory.
-   `./server.sh setup`: Only downloads the server JAR and accepts the EULA, without starting the server.
