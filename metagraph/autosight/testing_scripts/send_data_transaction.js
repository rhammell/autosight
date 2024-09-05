const { dag4 } = require("@stardust-collective/dag4");
const jsSha256 = require("js-sha256");
const jsSha512 = require("js-sha512");
const EC = require("elliptic");
const axios = require("axios");

const curve = new EC.ec("secp256k1");

const getEncoded = (message) => {
  const coded = JSON.stringify(message);
  return coded;
};

const serialize = (msg) => {
  const coded = Buffer.from(msg, "utf8").toString("hex");
  return coded;
};

const sha256 = (hash) => {
  return jsSha256.sha256(hash);
};

const sha512 = (hash) => {
  return jsSha512.sha512(hash);
};

const sign = async (privateKey, msg) => {
  const sha512Hash = sha512(msg);

  const ecSig = curve.sign(sha512Hash, Buffer.from(privateKey, "hex")); //, {canonical: true});
  return Buffer.from(ecSig.toDER()).toString("hex");
};

const sendDataTransactionsUsingUrls = async (
  globalL0Url,
  metagraphL1DataUrl
) => {

  const walletPrivateKey = dag4.keyStore.generatePrivateKey();
  const account1 = dag4.createAccount();
  account1.loginPrivateKey(walletPrivateKey);

  account1.connect({
    networkVersion: "2.0",
    l0Url: globalL0Url,
    testnet: true,
  });

  const message = {
    captureTime: (new Date()).getTime(),
    imageURL: "https://www.test.image.jpg",
    latitude: "0.0",
    longitude: "0.0",
    rewardAddress: ":your_wallet_address"
  };

  const encoded = getEncoded(message);
  const serializedTx = serialize(encoded);
  const hash = sha256(Buffer.from(serializedTx, "hex"));

  const signature = await sign(walletPrivateKey, hash);

  const publicKey = account1.publicKey;
  const uncompressedPublicKey =
    publicKey.length === 128 ? "04" + publicKey : publicKey;

  const body = {
    value: {
      ...message,
    },
    proofs: [
      {
        id: uncompressedPublicKey.substring(2),
        signature,
      },
    ],
  };
  try {
    console.log(`Transaction body: ${JSON.stringify(body)}`);
    const response = await axios.post(`${metagraphL1DataUrl}/data`, body);
    console.log(`Response: ${JSON.stringify(response.data)}`);
  } catch (e) {
    console.log("Error sending transaction", e);
  }
  return;
};

const sendDataTransaction = async () => {
const globalL0Url = ":your_global_l0_node_url";
  const metagraphL1DataUrl = ":your_metagraph_l1_data_url";

  await sendDataTransactionsUsingUrls(globalL0Url, metagraphL1DataUrl);
};

sendDataTransaction();
