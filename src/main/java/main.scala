import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.recommendation.ALS
import org.apache.spark.mllib.recommendation.Rating
object ALSExample {
  def main(args: Array[String]) {

    val conf=new SparkConf()
    val sc=new SparkContext(conf)
    // $example on$
    val rawData=sc.textFile("file:///home/lihang/mycode/mydata/movielens/ml-100k/u.data")
    val rawRatings=rawData.map(_.split("\t").take(3))
    val ratings=rawRatings.map{case Array(user,movie,rating)=>Rating(user.toInt,movie.toInt,rating.toDouble)}
    val model=ALS.train(ratings,50,10,0.01)
    val predictedRating=model.predict(789,123)
println(predictedRating)
    // Build the recommendation model using ALS on the training
  }
}
