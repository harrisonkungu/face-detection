# Document Scanner and Face Detection Application

## Overview
This application combines the functionalities of document scanning and face detection using advanced camera and image processing technologies. It enables users to scan documents and detect faces in real-time with high accuracy, leveraging tools like Android Jetpack Compose and ML Kit.

## Features
- **Real-Time Document Scanning**: Detects edges of documents in real-time and overlays a guide to assist users in positioning the document correctly.
- **Face Detection**: Identifies faces in the camera feed and highlights them with bounding boxes.
- **OCR Integration**: Extracts text from scanned documents using Optical Character Recognition (OCR).
- **Intuitive UI**: Designed with Jetpack Compose for a seamless and responsive user experience.

## Technologies Used
- **Kotlin**: Primary programming language.
- **Jetpack Compose**: For modern and reactive UI development.
- **CameraX**: For camera functionalities and real-time image analysis.
- **ML Kit (Vision API)**: For edge detection, face recognition, and OCR.
- **AndroidView**: Integrates CameraX PreviewView with Jetpack Compose.

## Setup Instructions
1. Clone the repository:
   ```bash
   git clone https://github.com/harrisonkungu/face-detection.git
   ```
2. Open the project in Android Studio.
3. Sync the project with Gradle.
4. Ensure you have the necessary dependencies in your `build.gradle` file:
   ```gradle
   implementation "androidx.camera:camera-view:1.2.0-alpha01"
   implementation "com.google.mlkit:text-recognition:16.0.0"
   implementation "androidx.compose.ui:ui:1.5.0"
   implementation "com.google.accompanist:accompanist-permissions:0.30.1"
   ```
5. Build and run the application on an Android device with a camera.

## Usage
- **Document Scanner**:
    1. Open the app and navigate to the Document Scanner feature.
    2. Align the document within the guide marks on the screen.
    3. Capture the image and perform OCR to extract text.

- **Face Detection**:
    1. Navigate to the Face Detection feature.
    2. Point the camera at the subject.
    3. The application will highlight detected faces with bounding boxes.

## Contribution
Contributions are welcome! Please follow these steps:
1. Fork the repository.
2. Create a new branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. Commit your changes:
   ```bash
   git commit -m "Add your message here"
   ```
4. Push to your branch:
   ```bash
   git push origin feature/your-feature-name
   ```
5. Open a Pull Request.

## License
This project is licensed under the MIT License
## Acknowledgements
- Google ML Kit for Vision API.
- Android Jetpack Compose and CameraX team for robust tools and libraries.

---

For questions or feedback, feel free to reach out to [harrisonkungu@yahoo.com].

**Screenshots**
<p align="center">
  <img src="https://github.com/harrisonkungu/face-detection/blob/main/assets/face-detect1.jpeg" alt="Screenshot 1" width="250" height = "500"/>
  <img src="https://github.com/harrisonkungu/face-detection/blob/main/assets/face-detect2.jpeg" alt="Screenshot 1" width="250" height = "500"/>
  <img src="https://github.com/harrisonkungu/face-detection/blob/main/assets/face-detect3.jpeg" alt="Screenshot 1" width="250" height = "500"/>
  <img src="https://github.com/harrisonkungu/face-detection/blob/main/assets/face-detect4.jpeg" alt="Screenshot 1" width="250" height = "500"/>
</p>
