import React, { useState, useEffect } from 'react';
import Map from './Map'
import Menu from './Menu'

function App() {
  const [imageRecords, setImageRecords] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isError, setIsError] = useState(false);
  const [selectedImageRecord, setSelectedImageRecord] = useState(null);

  // Fuction to request deivce data from API
  const fetchImageRecords = async () => {

    try {
      // Update loading state
      setIsLoading(true);

      // Get metagraph image records endpoint url
      const imagesUrl = process.env.REACT_APP_AUTOSIGHT_METAGRAPH_LO_IMAGES_URL;
      if (imagesUrl == '') {
        throw new Error(`Metagraph L0 URL must be defined`);
      }

      // Make API request
      const response = await fetch(imagesUrl);
      if (!response.ok) {
        throw new Error(`Failed to fetch data. Status: ${response.status}`);
      }
    
      // Parse and reformat images data
      const jsonData = await response.json();
      const imageRecordsData = jsonData.map(subArray => {
        const [, objectData] = subArray;        
        return objectData;
      });

      console.log(imageRecordsData)

      // Update app data 
      setImageRecords(imageRecordsData);

    } catch (error) {
      // Set error state to true
      setIsError(true);
      console.error("Error fetching data:", error.message);
    } finally {
      // Update lodaing state
      setIsLoading(false);
    }
  };

  // Fetch image records once when component loads
  useEffect(() => { 
    fetchImageRecords();
  }, []);

  return (
    <>
      <Menu 
        imageRecords={imageRecords}
        isLoading={isLoading}
        isError={isError}
        selectedImageRecord={selectedImageRecord}
        handleSelect={setSelectedImageRecord} 
      />
      <Map
        imageRecords={imageRecords}
        selectedImageRecord={selectedImageRecord}
        handleSelect={setSelectedImageRecord} 
      />
    </>
  );
}

export default App;
