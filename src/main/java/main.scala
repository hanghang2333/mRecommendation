import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.recommendation.ALS
import org.apache.spark.mllib.recommendation.Rating
import org.apache.spark.mllib.evaluation.RegressionMetrics
import org.jblas.DoubleMatrix
import org.apache.spark.mllib.evaluation.RankingMetrics
object ALSExample {
  def main(args: Array[String]) {

    val conf=new SparkConf()
    val sc=new SparkContext(conf)
    // $example on$
    val rawData=sc.textFile("file:///home/lihang/mycode/mydata/movielens/ml-10m/xaa")
    val rawRatings=rawData.map(_.split("::").take(3))
    val ratings=rawRatings.map{case Array(user,movie,rating)=>Rating(user.toInt,movie.toInt,rating.toDouble)}
    val model=ALS.train(ratings,50,10,0.01)
    val usersProducts=ratings.map{case Rating(user,product,rating)=>(user,product)}
    val predictions=model.predict(usersProducts).map{case Rating(user,product,rating)=>((user,product),rating)}
    val ratingsANdPredictions=ratings.map{case Rating(user,product,rating)=>((user,product),rating)}.join(predictions)
    val predictedAndTrue=ratingsANdPredictions.
      map{case ((user,product),(predicted,actual))=>(predicted,actual)}
    val regressionMetrics=new RegressionMetrics(predictedAndTrue)
    println(regressionMetrics.meanSquaredError)
    println(regressionMetrics.rootMeanSquaredError)
    val itemFactors=model.productFeatures.map{case (id,factor)=>factor}.collect()
    val s=new DoubleMatrix(Array(1.0,2.0,3.0))
    val itemMatrix=new DoubleMatrix(itemFactors)
    val imBroadcast=sc.broadcast(itemMatrix)
    val allRecs=model.userFeatures.map{case (userId,array)=>
    val userVector=new DoubleMatrix(array)
    val scores=imBroadcast.value.mmul(userVector)
    val sortedWithId=scores.data.zipWithIndex.sortBy(-_._1)
    val recommendedIds=sortedWithId.map(_._2+1).toSeq
      (userId,recommendedIds)}
    val userMovies=ratings.map{case Rating(user,product,rating)=>
      (user,product)}.groupBy(_._1)
    val predictedAndTrueForRanking=allRecs.join(userMovies).map{case (userId,(predicted,actualWithIds))=>
    val actual=actualWithIds.map(_._2)
      (predicted.toArray,actual.toArray)}
    val rankingMetrics=new RankingMetrics(predictedAndTrueForRanking)
    println(rankingMetrics.meanAveragePrecision)
    /*
    val MAPK2000=allRecs.join(userMovies).map{case(userId,(predicted,actualWithIds))=>
    val actual=actualWithIds.map(_._2).toSeq
    avgPrecisionK(actual,predicted,2000)}.reduce(_+_)/allRecs.count()
    println(MAPK2000)
    */
    //val predictedRating=model.predict(65314,3252)
//println(predictedRating)
    // Build the recommendation model using ALS on the training
  }
}
