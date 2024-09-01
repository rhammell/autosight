package com.my.shared_data.schema

import org.tessellation.currency.dataApplication.DataOnChainState

import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive

@derive(decoder, encoder)
final case class OnChain(
  images: Map[String, ImageRecord]
) extends DataOnChainState

object OnChain {
  val genesis: OnChain = OnChain(Map.empty)
}
