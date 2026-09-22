package com.dettle.app.di

import android.content.Context
import com.dettle.app.data.api.AIProviderFactory
import com.dettle.app.data.api.KeyPoolManager
import com.dettle.app.data.cloudflare.CloudflareClient
import com.dettle.app.data.db.DettleDatabase
import com.dettle.app.data.db.dao.DeploymentDao
import com.dettle.app.data.db.dao.MemoryDao
import com.dettle.app.data.db.dao.TaskLogDao
import com.dettle.app.data.settings.ApiKeyStore
import com.dettle.app.orchestrator.AgentBus
import com.dettle.app.orchestrator.CoderReviewerDebate
import com.dettle.app.orchestrator.MemoryInjector
import com.dettle.app.orchestrator.ReActLoop
import com.dettle.app.orchestrator.config.DettleAgentConfig
import com.dettle.app.orchestrator.mcp.McpClient
import com.dettle.app.orchestrator.policy.PolicyEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS  // Don't log bodies (contains API keys + prompts)
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)   // Long timeout for AI streaming
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideApiKeyStore(@ApplicationContext context: Context): ApiKeyStore =
        ApiKeyStore(context)

    @Provides
    @Singleton
    fun provideAIProviderFactory(
        keyStore: ApiKeyStore,
        client: OkHttpClient,
        json: Json
    ): AIProviderFactory = AIProviderFactory(keyStore, client, json)

    @Provides
    @Singleton
    fun provideKeyPoolManager(
        factory: AIProviderFactory,
        agentLogger: com.dettle.app.orchestrator.telemetry.AgentLogger
    ): KeyPoolManager = KeyPoolManager(factory, agentLogger)

    @Provides
    @Singleton
    fun provideGoogleDriveConnector(
        @ApplicationContext context: Context,
        keyStore: ApiKeyStore
    ): com.dettle.app.data.drive.GoogleDriveConnector =
        com.dettle.app.data.drive.GoogleDriveConnector(context, keyStore)

    @Provides
    @Singleton
    fun provideSelectorRegistry(
        client: OkHttpClient,
        json: Json
    ): com.dettle.app.data.webview.SelectorRegistry =
        com.dettle.app.data.webview.SelectorRegistry(client, json)

    @Provides
    @Singleton
    fun provideWebViewPool(
        @ApplicationContext context: Context,
        selectorRegistry: com.dettle.app.data.webview.SelectorRegistry,
        keyStore: ApiKeyStore
    ): com.dettle.app.data.webview.WebViewPool =
        com.dettle.app.data.webview.WebViewPool(context, selectorRegistry, keyStore)

    // ─── Agent Config System ───────────────────────────────────────────────────

    /**
     * Provides the project-level agent configuration.
     *
     * Starts with the hard-coded DEFAULT, which mirrors .agents/config.json.
     * At runtime, AgentConfigLoader fetches the live version from GitHub and
     * the agent system uses that. The DEFAULT is only used if GitHub is unreachable.
     *
     * This is a singleton so policies always work from the same config snapshot.
     */
    @Provides
    @Singleton
    fun provideDettleAgentConfig(): DettleAgentConfig = DettleAgentConfig.DEFAULT

    /**
     * Provides the PolicyEngine that enforces project rules before tool execution.
     * Injected into ReActLoop — evaluated before every ToolExecutor.execute() call.
     */
    @Provides
    @Singleton
    fun providePolicyEngine(config: DettleAgentConfig): PolicyEngine = PolicyEngine(config)

    // ─── Room Database ──────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideDettleDatabase(@ApplicationContext context: Context): DettleDatabase =
        DettleDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideMemoryDao(db: DettleDatabase): MemoryDao = db.memoryDao()

    @Provides
    @Singleton
    fun provideTaskLogDao(db: DettleDatabase): TaskLogDao = db.taskLogDao()

    @Provides
    @Singleton
    fun provideDeploymentDao(db: DettleDatabase): DeploymentDao = db.deploymentDao()

    // ─── Phase 3: Cloudflare + Multi-agent + MCP ───────────────────────────

    @Provides
    @Singleton
    fun provideCloudflareClient(
        keyStore: ApiKeyStore,
        client: OkHttpClient,
        json: Json
    ): CloudflareClient = CloudflareClient(keyStore, client, json)

    @Provides
    @Singleton
    fun provideAgentBus(): AgentBus = AgentBus()

    @Provides
    @Singleton
    fun provideMemoryInjector(
        memoryDao: MemoryDao,
        embeddingEngine: com.dettle.app.orchestrator.memory.EmbeddingEngine
    ): MemoryInjector = MemoryInjector(memoryDao, embeddingEngine)

    @Provides
    @Singleton
    fun provideMcpClient(client: OkHttpClient, json: Json): McpClient =
        McpClient(client, json)

    // ─── Phase 4: Overnight autonomous loop ────────────────────────────────

    @Provides
    @Singleton
    fun provideSelfImprovingSkillUpdater(
        memoryInjector: MemoryInjector
    ): com.dettle.app.orchestrator.overnight.SelfImprovingSkillUpdater =
        com.dettle.app.orchestrator.overnight.SelfImprovingSkillUpdater(memoryInjector)

    @Provides
    @Singleton
    fun provideTaskQueue(
        keyStore: ApiKeyStore,
        json: Json
    ): com.dettle.app.orchestrator.overnight.TaskQueue =
        com.dettle.app.orchestrator.overnight.TaskQueue(keyStore, json)

    @Provides
    @Singleton
    fun provideOvernightSummaryWriter(
        gitHubClient: com.dettle.app.data.github.GitHubClient,
        driveConnector: com.dettle.app.data.drive.GoogleDriveConnector,
        keyStore: ApiKeyStore,
        workspaceManager: com.dettle.app.data.workspace.LocalWorkspaceManager
    ): com.dettle.app.orchestrator.overnight.OvernightSummaryWriter =
        com.dettle.app.orchestrator.overnight.OvernightSummaryWriter(
            gitHubClient, driveConnector, keyStore, workspaceManager
        )

    // ─── Mode System ───────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideGoalDao(db: DettleDatabase): com.dettle.app.orchestrator.mode.GoalDao = db.goalDao()

    @Provides
    @Singleton
    fun provideGoalRepository(
        dao: com.dettle.app.orchestrator.mode.GoalDao,
        json: Json
    ): com.dettle.app.orchestrator.mode.GoalRepository =
        com.dettle.app.orchestrator.mode.GoalRepository(dao, json)

    @Provides
    @Singleton
    fun provideModeRouter(
        keyPoolManager: KeyPoolManager
    ): com.dettle.app.orchestrator.mode.ModeRouter =
        com.dettle.app.orchestrator.mode.ModeRouter(keyPoolManager)

    @Provides
    @Singleton
    fun provideContextCompressor(
        keyPoolManager: KeyPoolManager
    ): com.dettle.app.orchestrator.context.ContextCompressor =
        com.dettle.app.orchestrator.context.ContextCompressor(keyPoolManager)

    // ─── Personal AI System ───────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideProjectDao(db: DettleDatabase): com.dettle.app.data.db.dao.ProjectDao = db.projectDao()

    @Provides
    @Singleton
    fun provideConversationDao(db: DettleDatabase): com.dettle.app.data.db.dao.ConversationDao = db.conversationDao()

    @Provides
    @Singleton
    fun provideUserProfileDao(db: DettleDatabase): com.dettle.app.data.db.dao.UserProfileDao = db.userProfileDao()
}

