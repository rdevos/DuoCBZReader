package be.afront.reader
package state

case class AggregatePersistedState(preferences:Preferences, recentStates:RecentStates) extends Serializable
