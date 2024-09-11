# autosight
AutoSight is a metagraph-powered system that enables users to earn income while they drive, by contributing to a dataset of road images that AI companies pay to access.

The system is comprised of four components:

1. **Mobile App**: Users can use the AutoSight Android app on their phone to capture and upload images of the road from their vehicle, earning tokens for each upload. 

2. **Image Server**: Images are uploaded and saved to an image server that hosts them publicly. 

3. **Metagraph**: A custom metagraph works as the blockchain backend for the AutoSight system and is responsible for validating, storing, and publishing image data.

4. **Web App**: Customers in demand for road images can view them on the AutoSight web app, which features a menu of image previews and interactive map. 

# Demo Video
[![Video](https://img.youtube.com/vi/e6pI9gV_5bQ/0.jpg)](https://www.youtube.com/watch?v=e6pI9gV_5bQ)

# Project Setup
Follow the instructions below to set up and configure each of the AutoSight components.

## Dependencies
Install the required dependencies:
- [Python3](https://www.python.org/downloads/)
- [Node JS](https://nodejs.org/en)
- [Euclid Development Environment dependencies](https://docs.constellationnetwork.io/sdk/guides/quick-start/#install-dependencies)
- [Android Studio](https://developer.android.com/studio) with an Android phone

## Metagraph
Follow the Euclid Development Environment [quickstart guide](https://docs.constellationnetwork.io/sdk/guides/quick-start) to clone and setup the development environment, including the steps to configure docker and a github access token.

In a new terminal window, navigate to the cloned `euclid-development-environment` directory: 

```bash
cd euclid-development-environment
```

Install the `autosight` project from this repository into the development environment:  

```bash
scripts/hydra install-template autosight --repo https://github.com/rhammell/autosight --path metagraph
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
cd source/project/autosight/testing_scripts
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
- `<:your_metagraph_L0_url>/data-application/images`: Returns a list of image records, stored in the metagraph's on chain state. (Ex. `http://localhost:9200/data-application/images`)
- `<:your_metagraph_L0_url>/data-application/pending-rewards`: Returns a list of pending rewards records, stored in the metagraph's calculated state. (Ex. `http://localhost:9200/data-application/pending-rewards`)
- `<:your_metagraph_L0_url>/data-application/rewards`: Returns a list of addresses and their total minted tokens, stored in the metagraph's calculated state. (Ex. `http://localhost:9200/data-application/rewards`)

After testing is complete, restart the metagraph from its genesis snapshot to remove the dummy data: 

```bash
scripts/hydra stop
scripts/hydra start-genesis
```

## Clone AutoSight Repository
Clone the `autosight` code repository to your local machine: 

```bash
git clone https://github.com/rhammell/autosight.git
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

- `http://<public-ip>:5050` → `http://localhost:5050` (image server)
- `http://<public-ip>:9200` → `http://localhost:9200` (metagraph L0)
- `http://<public-ip>:9400` → `http://localhost:9400` (metagraph Data L1)

## Android App
Open Android Studio, select File → Open, select the `android_app/AutoSight` directory from the `autosight` cloned repository. 

In the upper left corner of the Project tool window, click the 'Project' dropdown menu, and select 'Android' from the options. 

Open the `AppConfig` file within the code editor by double clicking it in the file tree: app/kotlin+java/com.example.autosight/AppConfig

Update the following placeholder values in `AppConfig`, and save the file when complete: 
- `IMAGE_SERVER_ADDRESS`: Public URL of image server (ex. `http://<public-ip>:5050`)
- `METAGRAPH_L0_ADDRESS`: Public URL of metagraph L0 layer (ex. `http://<public-ip>:9200`)
- `METAGRAPH_DATA_L1_ADDRESS`: Public URL of metagraph Data L1 layer (ex. `http://<public-ip>:9400`)

Synch the project by selecting File → Sync Project with Gradle Files. Then, build the project by selecting Build → Make Project. 

Connect an Android device to the development computer via USB, then allow USB debugging on the phone when prompted. 

Select Run → Select Device, and select the connected device. 

Select Run → Run 'app' to install and run the AutoSight app on the connected Android phone. The app will remain installed on the device after it is disconnected. 

## Web App
In a new terminal window, navigate into the cloned `autosight` directory, then into the `web_app/autosight-explorer` subdirectory: 

```bash
cd autosight
cd web_app/autosight-explorer
```

Install the required Node JS packages: 

```bash
npm i
```

In a code editor, open the `.env` file, and update the following placeholder value, saving the file when complete: 
- `REACT_APP_AUTOSIGHT_METAGRAPH_LO_IMAGES_URL`: The `/data-application/images` endpoint of a metagraph node's Metagraph L0 URL (ex. `http://localhost:9200/data-application/images`) 

Start the development server: 

```bash
npm start
```

The AutoSight web application viewable by browsing to the default server address `http://localhost:3000`.
