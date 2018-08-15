package com.test

import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import org.apache.spark.sql.functions._

object DataImport extends Serializable {

  val sparkSession: SparkSession = init()

  import sparkSession.sqlContext.implicits._

  private def init(): SparkSession = {
    val sparkConf = new SparkConf().setAppName("spark-hive-test").setMaster("local[*]")

    SparkSession.builder()
      .config("hive.metastore.warehouse.dir", "user/hive/warehouse")
      .enableHiveSupport()
      .config(sparkConf).getOrCreate()
  }

  def main(args: Array[String]): Unit = {
    val logger = Logger.getLogger("org")
    logger.setLevel(Level.WARN)

    /**
      * Read & Write the file content to Hive using Spark
      */
    val inputDF = sparkSession.read.option("inferSchema", "true").option("header", "true").csv(args(0))

    inputDF.printSchema()
    inputDF.show(false)

    inputDF.write.mode(SaveMode.Append).saveAsTable("my_schema.data_table")


    hourlyAggregations(inputDF)

    yearlyAggregations(inputDF)


    inputDF.unpersist(true)

    sparkSession.close()
  }

  /**
    * Calculating Average number of recipes which are updated per hour and saving to Hive
    */
  private def hourlyAggregations(inputDF: DataFrame): Unit = {

    val avgDF = inputDF.withColumn("aggregation_key", concat(to_date($"updated_date"), lit("-"), hour($"updated_date")))
      .groupBy($"aggregation_key", $"recipe_name").agg(count($"aggregation_key").as("counter"))
    avgDF.show(false)

    avgDF.write.mode(SaveMode.Overwrite).saveAsTable("my_schema.hourly_updates_aggregation")

    avgDF.unpersist(true)
  }

  /**
    * Calculating Number of recipes which got updated at 10:00 clock in the entire year and saving to Hive
    */
  private def yearlyAggregations(inputDF: DataFrame): Unit = {

    val yearlyAvgDF = inputDF.withColumn("aggregation_key", concat(year($"updated_date"), lit("-"), hour($"updated_date")))
      .filter(hour($"updated_date") === 10)
      .groupBy($"aggregation_key").agg(count($"aggregation_key").as("counter"))
    yearlyAvgDF.show(false)

    yearlyAvgDF.write.mode(SaveMode.Overwrite).saveAsTable("my_schema.yearly_updates_aggregation")

    yearlyAvgDF.unpersist(true)
  }
}
