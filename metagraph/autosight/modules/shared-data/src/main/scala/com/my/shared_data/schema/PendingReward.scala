package com.my.shared_data.schema

import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive

import org.tessellation.schema.epoch.EpochProgress
import org.tessellation.schema.address.Address

@derive(decoder, encoder)
final case class PendingReward(
    rewardAddress      : Address,
    rewardAmount       : Long,
    rewardEpochProgress: EpochProgress
)