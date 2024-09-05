import { Box } from '@chakra-ui/react'
import L from "leaflet";
import { useEffect, useRef } from 'react';

import 'leaflet/dist/leaflet.css';
import '../css/iconStyle.css'

const Map = ({ imageRecords, selectedImageRecord, handleSelect }) => {
  const mapRef = useRef(null);
  const mapContainerRef = useRef(null);
  const groupRef = useRef(null);
  const menuOffset = 510

  const createMarkerStyle = (imageRecord) => {
    const isActive = imageRecord.id == selectedImageRecord;
    return  {
      radius: 10,
      fillColor: isActive ? "#FFFF00" : "#4395cf",
      color: "#35505e",
      weight: isActive ? 2 : 1,
      opacity: 1,
      fillOpacity: 1
    };
  };

  // Center map on map input layer
  const centerMap = (layer) => {
    if (layer instanceof L.CircleMarker) {
      const bounds = L.latLngBounds([layer.getLatLng()]);
      mapRef.current.fitBounds(bounds, {
        paddingTopLeft: [menuOffset, 0],
        maxZoom: mapRef.current.getZoom()
      });
    } else {
      const bounds = layer.getBounds();
      mapRef.current.fitBounds(bounds, {
        paddingTopLeft: [menuOffset, 0]
      });
    }
  }

  // Effect ran on initialization 
  useEffect(() => { 
    // Create map - store reference
    const map = L.map(mapContainerRef.current, {
      center: [37, -118],
      zoom: 4,
      attributionControl: false
    });
    mapRef.current = map;

    // Create layer group - store reference
    const featureGroup = L.featureGroup().addTo(map);
    groupRef.current = featureGroup;

    // OpenStreetMap layersadf
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '<a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    }).addTo(map);

    // Attribution
    L.control.attribution({
      position: 'topright',
      prefix: '<a href="https://leafletjs.com/">Leaflet</a>'
    }).addTo(map);

    // Zoom control
    L.control.zoom({
      position: 'topright'
    }).addTo(map);

    // Cleanup function
    return () => {
      mapRef.current.remove();
    };
  }, []);

  // Effect called when imageRecords data is updated
  useEffect(() => {
    if (imageRecords.length > 0) {

      // Clear layer group
      groupRef.current.clearLayers();

      // Loop through each product
      for (let imageRecord of imageRecords) {
        // Create map marker
        const lat = imageRecord.latitude;
        const lon = imageRecord.longitude
        const layer = L.circleMarker([lat, lon], createMarkerStyle(imageRecord)); // {icon: createMapIcon(imageRecord)})
        layer.imageRecord = imageRecord;
        groupRef.current.addLayer(layer)
          
        // Set click callback for maker layer
        layer.on('click', () => handleSelect(imageRecord.id))          
      }

      // Center map on group
      centerMap(groupRef.current)
    }
  }, [imageRecords])

  // Effect called each time selectedImageRecord is updated
  useEffect(() => {
    if (selectedImageRecord) {
      groupRef.current.eachLayer(layer => {
        if (layer.imageRecord.id == selectedImageRecord) {
          centerMap(layer);
        }
        layer.setStyle(createMarkerStyle(layer.imageRecord));
      });
    }
   }, [selectedImageRecord])

  return (
    <Box w={'100%'}>
      <Box
        ref={mapContainerRef}
        height='100vh'
        zIndex={1}
      />
    </Box>
  );
};

export default Map;