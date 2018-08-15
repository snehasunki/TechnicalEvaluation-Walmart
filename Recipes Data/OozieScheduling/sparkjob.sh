#!/bin/bash
spark-submit --class com.test.code-test.DataImport --master local /home/user/RecipesTest/code—test0.0.1-SNAPSHOT.jar /home/path_filetransfer/inputdata.csv
