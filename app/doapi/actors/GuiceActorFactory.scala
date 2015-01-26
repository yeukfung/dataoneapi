package doapi.actors

import com.google.inject.Inject
import akkaguice.ActorInstance

class GuiceActorFactory @Inject() (val jobsActor: ActorInstance[JobsActor]) 

