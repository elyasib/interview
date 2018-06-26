package forex

package object processes {
  type Rates[F[_]] = rates.Processes[F]
  final val Rates = rates.Processes
}
