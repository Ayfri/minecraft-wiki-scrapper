import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
enum class SnapshotType {
	@SerialName("snapshot")
	SNAPSHOT,
	
	@SerialName("pre-release")
	PRE_RELEASE,
	
	@SerialName("candidate")
	RELEASE_CANDIDATE,
	
	@SerialName("release")
	RELEASE;
	
	companion object {
		fun resolve(type: String?) = when (type?.lowercase()?.replace(Regex("\\s"), "-")) {
			"snapshot", "test-build", "preview", "experimental-snapshot" -> SNAPSHOT
			"pre-release" -> PRE_RELEASE
			"release-candidate" -> RELEASE_CANDIDATE
			else -> RELEASE
		}
	}
}

@Serializable
data class Snapshot(
	var name: String = "",
	var releaseTime: Long? = null,
	var description: String = "",
	var downloadClient: String? = null,
	var downloadJSON: String? = null,
	
	@Serializable
	var snapshotType: SnapshotType? = null,
	
	@Transient
	var snapshotFor: String? = null
) {
	init {
		description = description.replace("\n", "\\n")
	}
}

@Serializable
data class Version(
	var name: String = "",
	var releaseTime: Long? = null,
	var description: String = "",
	var snapshots: List<Snapshot> = emptyList()
) {
	init {
		description = description.replace("\n", "\\n")
	}
}
