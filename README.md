# Kryptonite
__Kryptonite__ generates and stores an SSH key pair on a mobile phone. The
Kryptonite app is paired with one or more workstations by scanning a QR code
presented in the terminal. When using SSH from a paired workstation, the
workstation requests a private key signature from the phone. The user then
receives a notification and chooses whether to allow the SSH login.

# Build Instructions
1) Follow build instructions in `libsodium-jni`
2) Follow build instructions in `ssh-wire`
3) Open project in Android Studio

# Have an iPhone?
The iOS implementation is located [here](https://github.com/kryptco/kryptonite-ios).
