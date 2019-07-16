package io.digitallibrary.bookapi.integration

import java.io.{BufferedOutputStream, FileOutputStream}

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.{AWSCredentials, AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions
import com.amazonaws.services.apigateway.{AmazonApiGateway, AmazonApiGatewayClientBuilder}
import io.digitallibrary.bookapi.{TestEnvironment, UnitSuite}

import scala.util.{Failure, Success}

class MediaApiClientTest extends UnitSuite with TestEnvironment {


  override val awsApiGatewayClient: AmazonApiGateway = AmazonApiGatewayClientBuilder
    .standard()
    .withClientConfiguration(new ClientConfiguration().withTcpKeepAlive(false))
    .withRegion(Regions.EU_CENTRAL_1)
    .build()

  override val gdlClient: GdlClient = new GdlClient
  val client = new MediaApiClient

  test("parsing ContentType works with only contentType") {
    val downloadedImageTry = client.downloadImage(id = 1, format = Some("jpg"))
    downloadedImageTry match {
      case Failure(ex) => ex.printStackTrace()
      case Success(downloadedImage) => {
        val filename = s"/Users/kes/temp/${downloadedImage.filename}"
        println(s"Filename will be $filename")
        val bos = new BufferedOutputStream(new FileOutputStream(filename))
        bos.write(downloadedImage.bytes)
        bos.close()
      }
    }

  }
}
