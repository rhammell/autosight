package com.my.shared_data.schema

import cats.effect.Async
import cats.implicits.toFunctorOps

import org.tessellation.schema.address.Address
import org.tessellation.security.Hasher

import com.my.shared_data.schema.Updates.ImageUpdate

import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive

@derive(decoder, encoder)
final case class ImageRecord(
  id: String,
  deviceAddress: Address,
  captureTime: Long,
  imageURL: String,
  latitude: Double,
  longitude: Double
)

object ImageRecord {

  def generateId[F[_]: Async: Hasher](update: ImageUpdate): F[String] =
    Hasher[F].hash(update).map(_.value)

}