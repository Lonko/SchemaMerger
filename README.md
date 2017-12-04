# SchemaMerger

## Requirements

* src/main/resources should countain a folder with the dataset; the dataset should contain a folder for each website, each containing one file per product category. the folder specification_example contains an example of the json file.
* src/main/resources should contain a file id2category2urls.json that tracks the record linkage between product pages; an example file is provided in the resources folder.
* MongoDB must be installed on the machine in order to load the dataset on a db.
* The project uses R for the classification; as such, R needs to be installed, as well as the packages "caret" and "rJava".
* It's necessary to set the library path for jri as VM argument in the IDE. (example: "-Djava.library.path=.:/usr/lib/R/site-library/rJava/jri/")
* It's necessary to set up the environment variables (based on the folder in which R has been installed):
    * R_DOC_DIR
    * R_HOME
    * R_INCLUDE_DIR
    * R_SHARE_DIR
