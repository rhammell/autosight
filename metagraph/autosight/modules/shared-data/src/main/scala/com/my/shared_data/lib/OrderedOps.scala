package com.my.shared_data.lib

import com.my.shared_data.schema.Updates._

object OrderedOps {
  object implicits {
    implicit val ImageUpdateOrdering: Ordering[ImageUpdate] =
      new Ordering[ImageUpdate] {
        def compare(x: ImageUpdate, y: ImageUpdate): Int = {
          x.captureTime.compare(y.captureTime)
        }
      }
  }
}
