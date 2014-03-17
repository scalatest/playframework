/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.tests.specs2

import org.specs2.mock._
import org.specs2.mutable._

import java.util._

case class Data(retrievalDate: java.util.Date)

trait DataService {
  def findData: Data
}

class MyService {
  def dataService: DataService = null // implementation reference...

  def isDailyData: Boolean = {
    val retrievalDate = Calendar.getInstance
    retrievalDate.setTime(dataService.findData.retrievalDate)

    val today = Calendar.getInstance()

    (retrievalDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)
      && retrievalDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR))
  }
}

// #scalaws-mockito
class ExampleMockitoSpec extends Specification with Mockito {

  "MyService#isDailyData" should {
    "return true if the data is from today" in {
      val mockDataService = mock[DataService]
      mockDataService.findData returns Data(retrievalDate = new java.util.Date())

      val myService = new MyService() {
        override def dataService = mockDataService
      }

      val actual = myService.isDailyData
      actual must equalTo(true)
    }
  }
}
// #scalaws-mockito
