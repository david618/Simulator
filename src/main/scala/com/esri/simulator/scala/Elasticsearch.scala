package com.esri.simulator.scala


import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.{Seconds, StreamingContext, Time}
import org.elasticsearch.spark.rdd.EsSpark

/**
  * This class watches a specified folder for files (expecting csv files of type simFile)
  * Fields from simFile mapped to case class simFile and loaded into Elasticsearch
  */
object CsvElasticsearch {

  def main(args: Array[String]) {

    println("START")

    SimLogging.setStreamingLogLevels()

    val appName = "CsvSparkElasticsearch"

    val numargs = args.length

    // example: a3:9200 elasticsearch simulator simfile /home/david/streamfiles
    // example: a2:9200 elasticsearch simulator simfile D:/streamfiles

    if (numargs != 5) {
      System.err.println("Usage: CsvElasticsearch <esNodes> <clusterName> <indexName> <typeName> <folder>")
      System.err.println("            esNode(s): elasticsearth server port, e.g. localhost:9200")
      System.err.println("          clusterName: Elasticsearch Cluster Name")
      System.err.println("            indexName: Index name for Elasticsearch, e.g. simulator")
      System.err.println("             typeName: Type name for Elasticsearch, e.g. simfile")
      System.err.println("               folder: Watch this folder for new simFile csv files")
      System.exit(1)
    }
    // spark-submit --class com.esri.simulator.scala.CsvElasticsearch --master local[8] Simulator-jar-with-dependencies.jar a3:9200 elasticsearch simulator simfile /home/david/streamfiles


    val Array(esNodes,esClusterName,esIndexName,esTypeName,folder) = args

    val sparkConf = new SparkConf().setAppName(appName)
    // Uncomment the following line to run from IDE
    sparkConf.setMaster("local[8]")

    sparkConf.set("es.index.auto.create", "true").set("es.cluster.name",esClusterName).set("es.nodes", esNodes)

    case class simfile(rt: String, dtg: String, lon: Double, lat: Double)

    val ssc = new StreamingContext(sparkConf, Seconds(2))

    val ds = ssc.textFileStream(folder)

    var st = System.currentTimeMillis()

    ds.foreachRDD((rdd: RDD[String], time: Time) => {
      if (rdd.count() > 0) {
        st = System.currentTimeMillis();
      }
      val jsonRDD = rdd.map(_.split(",")).map(e => simfile(e(0), e(2),e(4).toDouble,e(5).toDouble))
      EsSpark.saveToEs(jsonRDD, esIndexName + "/" + esTypeName)
      val delta = System.currentTimeMillis() - st
      val rate = 1000.0 * rdd.count.toDouble / delta.toDouble
      if (rdd.count() > 0) {
        println()
        println("Time %s: Elasticsearch sink (saved %s total records at rate of %s)".format(time, rdd.count(), rate))
      } else {
        println("Time %s ".format(time))
      }
    })

    ssc.start()
    ssc.awaitTermination()

    println("END")
  }

}


/**
  * This class watches a specified folder for files (expecting file with json lines)
  * The lines are loaded into Elasticsearch
  */
object JsonElasticsearch {

  def main(args: Array[String]) {
    println("START")

    SimLogging.setStreamingLogLevels()

    val appName = "JsonSparkToElasticsearch"

    val numargs = args.length

    // example: a3:9200 elasticsearch simulator simfile /home/david/streamfiles

    if (numargs != 5) {
      System.err.println("Usage: JsonElasticsearch <esNodes> <clusterName> <indexName> <typeName> <folder>")
      System.err.println("            esNode(s): elasticsearth server port, e.g. localhost:9200")
      System.err.println("          clusterName: Elasticsearch Cluster Name")
      System.err.println("            indexName: Index name for Elasticsearch, e.g. simulator")
      System.err.println("             typeName: Type name for Elasticsearch, e.g. simfile")
      System.err.println("               folder: Watch this folder for new files with lines of json to load")
      System.exit(1)
    }


    // spark-submit --class com.esri.simulator.scala.JsonElasticsearch --master local[8] Simulator-jar-with-dependencies.jar a3:9200 elasticsearch simulator simfile2 /home/david/streamfiles

    val Array(esNodes,esClusterName,esIndexName,esTypeName,folder) = args

    val sparkConf = new SparkConf().setAppName(appName)
    // Uncomment the following line to run from IDE
    //sparkConf.setMaster("local[8]")

    sparkConf.set("es.index.auto.create", "true").set("es.cluster.name",esClusterName).set("es.nodes", esNodes)
    // When connecting to spark running on my a3 vm from host; I was getting a connection error; Adding the following
    // lines it created the index/type however it still timedout with error
    // The real fix was to change elasticsearch.yml config and specified the host  network.host: 192.168.56.131
    //sparkConf.set("es.nodes.discovery", "false");
    //sparkConf.set("es.nodes.data.only", "false");

    val ssc = new StreamingContext(sparkConf, Seconds(2))

    val ds = ssc.textFileStream(folder)

    var st = System.currentTimeMillis()

    ds.foreachRDD((rdd: RDD[String], time: Time) => {
      if (rdd.count() > 0) {
        st = System.currentTimeMillis();
      }
      EsSpark.saveJsonToEs(rdd, esIndexName + "/" + esTypeName)
      val delta = System.currentTimeMillis() - st
      val rate = 1000.0 * rdd.count.toDouble / delta.toDouble
      if (rdd.count() > 0) {
        println()
        println("Time %s: Elasticsearch sink (saved %s total records at rate of %s)".format(time, rdd.count(), rate))
      } else {
        //println("Time %s ".format(time))
      }
    })

    ssc.start()
    ssc.awaitTermination()

    println("END")

  }
}
