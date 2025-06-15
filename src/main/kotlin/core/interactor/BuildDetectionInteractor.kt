package core.interactor

/*
 * Detects skill trees and builds for each player over time. Makes available the most recently detected build
 * for use in other parts of the application. Uses SkillTreeDefinitions to identify which trees each skill belongs to.
 */
class BuildDetectionInteractor : Interactor() {

    override suspend fun interact() {
        // This method will be called every 3 seconds to detect the current build.
        // Implement the logic to detect the build here.
        // For example, you might check the current skill tree or other game state.
    }
}