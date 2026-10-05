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
    }

    @Test
    fun testReadOnlyToolsDoNotIncludeMutations() {
        val readOnlyNames = AgentTools.READ_ONLY.map { it.name }.toSet()

        assertFalse(readOnlyNames.contains(AgentTools.GITHUB_CREATE_OR_UPDATE_FILE.name))
        assertFalse(readOnlyNames.contains(AgentTools.GITHUB_CREATE_PULL_REQUEST.name))
        assertFalse(readOnlyNames.contains(AgentTools.CLOUDFLARE_DEPLOY_WORKER.name))
        assertFalse(readOnlyNames.contains(AgentTools.CLOUDFLARE_CREATE_KV.name))
        assertFalse(readOnlyNames.contains(AgentTools.FILE_DELETE.name))
        assertFalse(readOnlyNames.contains(AgentTools.SHELL_EXECUTE.name))
    }

    @Test
    fun testAllToolsIncludeBothReadAndWrite() {
        val allNames = AgentTools.ALL.map { it.name }.toSet()

        assertTrue(allNames.contains(AgentTools.GITHUB_LIST_REPOS.name))
        assertTrue(allNames.contains(AgentTools.GITHUB_CREATE_OR_UPDATE_FILE.name))
        assertTrue(allNames.contains(AgentTools.CLOUDFLARE_DEPLOY_WORKER.name))
        assertTrue(allNames.contains(AgentTools.IMAGEKIT_UPLOAD.name))
    }
}
