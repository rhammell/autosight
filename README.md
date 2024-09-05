# autosight
A metagraph powered Android app that allows users to earn crypto tokens while driving

# Overview Video

# Project Setup
The following instructions cover the setup and configuration process for each AutoSight component. 

## Dependencies
Install the following required dependencies:
- [Python3](https://www.python.org/downloads/)
- [Node JS](https://nodejs.org/en)
- [Euclid Development Enivonment dependencies](https://docs.constellationnetwork.io/sdk/guides/quick-start/#install-dependencies)
- [Android Studio](https://developer.android.com/studio) with an Android phone

## Clone
Clone the `autosight` code repository to your local machine: 

```bash
git clone https://github.com/rhammell/autosight.git
```

## Metagraph
Follow the Euclid Development Environment [quickstart guide](https://docs.constellationnetwork.io/sdk/guides/quick-start) to clone and setup the development environment, including the steps to configure docker and a github access token.

In a new terminal window, navigate to the cloned `euclid-development-environment` directory: 

```bash
cd euclid-development-environment
```

Install the `autosight` project from this repository into the development environment:  

```bash
scripts/hydra install-template autosight --repo https://github.com/rhammell/autosight --path metagr
aph
```

Build the `autosight` project containers: 

```bash
scripts/hydra build
```

Start the metagraph components from a genesis snapshot: 

```bash
scripts/hydra start-genesis
```

When this process is complete, the terminal will display metagraph information, including the URLs for each metagraph node layer.

### Testing 
Test sending custom data to the deployed metagraph using the included `send_data_transaction.js` script. This script creates dummy image record data, cryptographically signs it, and sends the structured data to the metagraph's data L1 layer to be processed.

From the `euclid-development-environment` directory, navigate into the `testing_scripts` directory: 

```bash
cd /source/project/autosight/testing_scripts
```

Install the required Node JS packages: 

```bash
npm i
```

In a code editor, open the `send_data_transaction.js` file, and update the following placeholder values, saving the file when complete: 
- `:your_global_l0_node_url`: A metagraph node's Global L0 URL, copied from the terminal output. (ex. `http://localhost:9000`)
- `:your_metagraph_l1_data_url`: A metagraph node's Data L1 URL, copied from the terminal output. (ex. `http://localhost:9400`) 
- `:your_wallet_address`: A valid DAG wallet address, obtainable from the Stargazer wallet. 

Run the test script: 

```bash
node send_data_transaction.js
```

The script will output the hash value returned by the metagraph after successfully validating and combining the dummy data. Reward tokens are minted for the provided wallet address in the script. 

In a browser, navigate to the following custom endpoints to view the metagraph's state:
- `<:your_metagraph_L0_url>/data-application/images`: Returns a list of image records, stored in the metagraph's on chain state
- `<:your_metagraph_L0_url>/data-application/rewards`: Returns a list of token rewards records, stored in the metagraph's calculated state. 

After testing is complete, restart the metagraph from its genesis snapshot to remove the dummy data: 

```bash
scripts/hydra stop
scripts/hydra start-genesis
```

## Image Server
In a new terminal window, navigate into the cloned `autosight` directory, then into the `image_upload` subdirectory: 

```bash
cd autosight
cd image_upload
```

Install the required Python packages:

```bash
pip3 install -r requirements.txt
```

Launch the server: 

```bash
python3 server.py
```

The server runs on the default port (5050) and is now available at `http://localhost:5050`.


## Public URL Requirement
The AutoSight Android app relies on connections to the metagraph nodes and image upload server. However, since the app is intended to be used while in a vehicle, the phone will not be connected to the same local network as the host machine running the metagraph and image server, and must access them through public URLs. 

This can be accomplished by either deploying the metagraph and image server to a cloud service provider, or enabling port forwarding on the host machine's local network router. 

If using port forwarding, configure your router to forward inbound requests from its public IP address to the appropriate ports on your local machine. For example:

- http://<public-ip>:5050 → http://localhost:5050 (image server)
- http://<public-ip>:9200 → http://localhost:9200 (metagraph L0)
- http://<public-ip>:9400 → http://localhost:9400 (metagraph Data L1)

## Android App
Open Android Studio, select File->Open, select the `android_app/AutoSight` directory from the `autosight` cloned repository. 

In the upper left cornder of the Project tool window, click the 'Project' dropdown menu, and select 'Android' from the options. 

Open the `AppConfig` file within the code editor by double cliking it in the file tree: app/kotlin+java/com.example.autosight/AppConfig

Update the following placeholder values in `AppConfig`, and save the file when complete : 
- `IMAGE_SERVER_ADDRESS`:
- `METAGRAPH_L0_ADDRESS`: 
- `METAGRAPH_DATA_L1_ADDRESS`:

Synch the project by selecting File -> Sync Project with Gradle Files. Then, bulid the project by selecting Build -> Make Project. 

Connect an Android device to the development computer via USB, then allow USB debugging on the phone when prompted. 

Selct Run -> Select Device, and select the connected device. 

Select Run -> Run 'app' to install and run the AutoSight app on the connected Android phone. The app will remain installed on the device after it is disconnected. 

## Web App

# Usage
