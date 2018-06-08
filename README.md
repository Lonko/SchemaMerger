# SchemaMerger

## Requirements for Classification

* src/main/resources should countain a folder with the dataset; the dataset should contain a folder for each website, each containing one file per product category. the folder specification_example contains an example of the json file.
* src/main/resources should contain a file id2category2urls.json that tracks the record linkage between product pages; an example file is provided in the resources folder.
* MongoDB must be installed and running on the machine in order to load the dataset on a db.
* The project uses R for the classification; as such, R needs to be installed, as well as the packages "caret" and "rJava".
* It's necessary to set the library path for jri as VM argument in the IDE. (example: "-Djava.library.path=.:/usr/lib/R/site-library/rJava/jri/")
* It's necessary to set up the environment variables (based on the folder in which R has been installed):
    * R_DOC_DIR
    * R_HOME
    * R_INCLUDE_DIR
    * R_SHARE_DIR
    
## Requirements for Dataset Generation
 * MongoDB must be installed and running on the machine in order to load the generated dataset on a db.
 * In the properties.config file change the parameter "mongoURI" with the URI to the mongoDB server.
 * The parameters "databaseName" and "category" can be changed freely. Each execution generates a dataset with a single category. To generate several categories only change the "categories" parameter, "databaseName" should remain the same; at the beginning of the execution do not choose to delete the database.  

## TO-DO
* [x] Classifier training method (currently done with a simple R script)
* [ ] Dictionary based String generator
* [x] Sources creation from synthetic catalog  :exclamation:
* [x] Error Rate curve for the generated sources
* [x] Automatic evaluation of classification on synthetic dataset  :exclamation:
* [ ] Compute on dataset the order of sources

## Getting started + application description

**Premise**: results of Agrawal depends on order with which it computes each sources. If they are provided in order of linkage descendent, its results should be better. The problem is that an efficient way for calculating this order has not been implemented yet. For this reason this order should be provided in some way.

**Two main entry points:**
* launchers.SyntheticDatasetGenerator#main --> generate Synthetic dataset and add to Mongo
* launchers.Cohordinator#main --> Launch Agrawal algorithm. 2 possibilities (depends on user input y/n): 
  * Launch on **real** dataset --> order of sources should be provided as input (currently there is a constant in Cohordinator
  * Launch on **synthetic** dataset --> launches the S.D. generation, then Agrawal. This cannot be done currently on different steps, as SD object keeps source order in an instance variable. 

**Parameters**: Run parameters should provide a token PROD, TEST or CUSTOM, so that config\_prod.json, config\_test.json or config\_custom.json will be used (custom is in gitignore).


