package be.afront.reader
package state

case class AggregatePersistedState(preferences:AppPreferences, recentStates:RecentStates) extends Serializable
