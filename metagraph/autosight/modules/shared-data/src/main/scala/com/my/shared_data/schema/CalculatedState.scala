package com.my.shared_data.schema

import org.tessellation.currency.dataApplication.DataCalculatedState

import org.tessellation.schema.address.Address

import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive

@derive(decoder, encoder)
final case class CalculatedState(
  totalRewards  : Map[Address, Long],
  pendingRewards: List[PendingReward]
) extends DataCalculatedState

object CalculatedState {
  val genesis: CalculatedState = CalculatedState(Map.empty, List.empty)
}
