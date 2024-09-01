package com.my.shared_data.schema

import org.tessellation.currency.dataApplication.DataUpdate

import org.tessellation.schema.address.Address

import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive

object Updates {

  @derive(decoder, encoder)
  case class ImageUpdate(
    captureTime: Long,
    imageURL: String,
    latitude: String,
    longitude: String,
    rewardAddress: Option[Address],
  ) extends DataUpdate

}
