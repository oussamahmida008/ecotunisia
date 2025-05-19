# EcoTunisia (EcoExplorer)

![EcoTunisia Logo](app/src/main/res/mipmap-xxxhdpi/ic_launcher.png)

## Overview

EcoTunisia (EcoExplorer) is a comprehensive Android application designed to promote environmental awareness and conservation in Tunisia. The app serves as a platform for users to discover, share, and engage with eco-friendly locations and environmental initiatives across Tunisia.

## Features

### 🔐 Authentication
- User registration and login using Firebase Authentication
- Secure account management
- Profile customization with language preferences

### 🌦️ Weather Information
- Real-time weather data for locations across Tunisia
- Detailed weather forecasts including temperature, humidity, and wind conditions
- Weather-based recommendations for outdoor activities
- Data provided by WeatherAPI

### 🗺️ Interactive Map
- Discover eco-friendly locations across Tunisia using OSMDroid
- Add and share new environmental points of interest
- View detailed information about each location
- Copy location details to share with others
- Filter locations by type and user contributions
- Photo upload capability for locations
- Proper location permission handling

### 💬 Community Chat
- Join topic-based channels to discuss environmental issues
- Create new channels for specific environmental topics
- Share images and location information in chats
- Phone number detection with call and message functionality
- Real-time messaging with Firebase backend
- Image sharing capabilities

### 👤 User Profiles
- Customizable user profiles with profile pictures
- Language preferences (English/French)
- Dark mode support
- Account management and settings

## Technical Details

### Architecture
- Built using MVVM (Model-View-ViewModel) architecture
- Repository pattern for data management
- LiveData for reactive UI updates
- Coroutines for asynchronous operations
- Clean separation of concerns

### Backend & Services
- Firebase Authentication for user management
- Firestore for database storage
- Firebase Storage for image storage
- WeatherAPI integration for weather data
- OSMDroid for map functionality

### Key Components
- Data Binding for UI interactions
- Navigation Component for app navigation
- Glide for image loading and caching
- Material Design components for modern UI
- Location services with proper permission handling

### Multilingual Support
- Dynamic language switching without app restart
- Resource files for both French and English
- Localized content throughout the app

## Setup Instructions

1. Clone the repository
2. Open the project in Android Studio
3. Connect to Firebase using the provided google-services.json
4. Build and run the application on an emulator or physical device

## Requirements

- Android 8.0 (API level 26) or higher
- Google Play Services
- Internet connection
- Location services (for map functionality)

## API Keys
- WeatherAPI key: 489a31d76aef46199a7214517251805

## Firebase Configuration
The app uses Firebase for authentication, real-time database, and storage. The configuration is included in the google-services.json file.

## Future Enhancements

- Environmental news feed
- Carbon footprint calculator
- Volunteer event organization
- Gamification elements for environmental actions
- Integration with environmental NGOs in Tunisia

## Contributors

This project was developed by:

- **Oussama Hmida** - Lead Developer
- **Saad Arafet Mahfoudh** - UI/UX Design & Backend Integration
- **Samar Jellasi** - Content & Environmental Research

## License

This project is licensed for educational purposes only.

---

For questions, feedback, or contributions, please contact: [oussamahmida008@gmail.com](mailto:oussamahmida008@gmail.com)
