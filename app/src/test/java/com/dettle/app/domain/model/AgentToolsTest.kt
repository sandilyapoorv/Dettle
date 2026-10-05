package com.dettle.app.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentToolsTest {

    @Test
    fun testReadOnlyToolsIncludeComprehensiveGitHubReadTools() {
        val readOnlyNames = AgentTools.READ_ONLY.map { it.name }.toSet()

        assertTrue(readOnlyNames.contains(AgentTools.GITHUB_LIST_REPOS.name))
        assertTrue(readOnlyNames.contains(AgentTools.GITHUB_GET_REPO.name))
        assertTrue(readOnlyNames.contains(AgentTools.GITHUB_LIST_BRANCHES.name))
        assertTrue(readOnlyNames.contains(AgentTools.GITHUB_LIST_COMMITS.name))
        assertTrue(readOnlyNames.contains(AgentTools.GITHUB_LIST_ISSUES.name))
        assertTrue(readOnlyNames.contains(AgentTools.GITHUB_READ_FILE.name))
        assertTrue(readOnlyNames.contains(AgentTools.GITHUB_MAP_REPO.name))
        assertTrue(readOnlyNames.contains(AgentTools.GITHUB_POLL_RUN.name))
    }

    @Test
    fun testReadOnlyToolsDoNotIncludeMutations() {
        val readOnlyNames = AgentTools.READ_ONLY.map { it.name }.toSet()

        assertFalse(readOnlyNames.contains(AgentTools.GITHUB_CREATE_BRANCH_PR.name))
        assertFalse(readOnlyNames.contains(AgentTools.GITHUB_TRIGGER_ACTION.name))
        assertFalse(readOnlyNames.contains(AgentTools.CLOUDFLARE_DEPLOY_PREVIEW.name))
        assertFalse(readOnlyNames.contains(AgentTools.CLOUDFLARE_PUBLISH_WORKER.name))
        assertFalse(readOnlyNames.contains(AgentTools.WORKSPACE_WRITE_FILE.name))
        assertFalse(readOnlyNames.contains(AgentTools.WORKSPACE_DELETE_FILE.name))
    }

    @Test
    fun testAllToolsIncludeBothReadAndWrite() {
        val allNames = AgentTools.ALL.map { it.name }.toSet()

        assertTrue(allNames.contains(AgentTools.GITHUB_LIST_REPOS.name))
        assertTrue(allNames.contains(AgentTools.GITHUB_CREATE_BRANCH_PR.name))
        assertTrue(allNames.contains(AgentTools.CLOUDFLARE_DEPLOY_PREVIEW.name))
        assertTrue(allNames.contains(AgentTools.CLOUDFLARE_PUBLISH_WORKER.name))
        assertTrue(allNames.contains(AgentTools.WORKSPACE_WRITE_FILE.name))
    }
}
