# Nimrod/OK
This is the authoritative repository of the Nimrod/OK source code.

## Installation
There are several ways to install Nimrod/OK.

### Kepler

Install Kepler via SVN - recommended and easiest method - the embedded version of Nimrod/OK is guaranteed to be the most stable current version. See [here](https://kepler-project.org/developers/teams/build/documentation/build-system-instructions) for instructions.

### Manual Installation

Manual installation should be done when you want the latest developments and bleeding-edge, unstable changes, or wish to help develop Nimrod/OK.

* Install [Kepler 2.4 or 2.5](https://kepler-project.org/users/downloads) and [Gradle](https://gradle.org/install/).
* Clone the repository
* Set your ```KEPLER``` environment variable to wherever Kepler installed.
* Install Nimrod/K by executing ```nimrodk-2.0.2.jar``` from the clone directory, this requires the ```KEPLER``` environment variable be set.
* Run ```/path/to/repo$ gradle install```, this will build and install all the Nimrod/OK components and inject them into your Kepler installation.
* Delete Kepler's cache and data folder, usually located at ```~/.kepler``` and ```~/KeplerData``` respectively.

### Advanced Installation

Advanced installation should only be used for internal development, testing, and (ab)use of Nimrod/OK.

Nimrod/OK has two primary installation modes _install_, and _seed_.
* _install_ - Install Nimrod/OK into an existing Kepler installation
  - This will copy all Nimrod/OK files and inject ontologies and actor definitions into Kepler's configuration. This is the safest way.
* _seed_ - Seed a source installation of Kepler (SVN checkout) with the current state of the Nimrod/OK repository.
  - This will snapshot the current state of the repository and copy it into a Kepler source installation, ready to be pushed.

Both modes require the ```KEPLER``` environment variable to be set to the Kepler installation directory. Safeguards have been put in place to prevent misuse -- You can't _install_ to a Kepler source installation, and you can't _seed_ a binary installation. Nimrod/OK's build system will detect this and throw an error.

## License
3-Clause BSD License [(link)](https://opensource.org/licenses/BSD-3-Clause)