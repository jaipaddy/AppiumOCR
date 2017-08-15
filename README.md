# AppiumOCR
Using SikuliX OCR API with Appium

Disclaimer: I have modified OCR.java from https://github.com/Simon-Kaz/AppiumFindByImage to add some more features. 

Why shoud I use this?
If your iPhone/Android mobile app does not have an id, name or accessibilityID then chances are that you cannot interact with that element on the screen, especially if Xpath is unreliable or not available. Image recognition with SikuliX api comes to the rescue in this case.

What do I need?
1. Appium server 
2. Appium java client
3. SikuliX API Jar v1.1.1 - http://sikulix.com/

How to use this class?
1. Take a screenshot of the screen of interest on your iPhone
2. Use the Photos app on your Macbook to crop the screenshot and save the image you want to click using Appium (file.jpg). No other screenshot utility worked with SikuliX except Photos
3. Note that the coordinates of the matched image need to be divided by 2 to account for the "Retina" display on iPhones since they pack twice the number of pixels.
