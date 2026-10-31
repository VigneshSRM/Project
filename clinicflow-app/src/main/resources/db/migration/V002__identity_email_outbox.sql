SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

-- The outbox worker needs the rendered message until it is delivered. These are
-- nullable so existing EmailLogs rows (if any) remain valid.
ALTER TABLE dbo.EmailLogs
  ADD subject NVARCHAR(300) NULL,
      body NVARCHAR(MAX) NULL,
      next_attempt_at DATETIME2(3) NULL;
GO

-- A token hash is looked up for every verification/reset request. It must be
-- unique so two tokens can never collide or be replayed ambiguously.
CREATE UNIQUE INDEX UX_AuthTokens_token_hash
  ON dbo.AuthTokens(token_hash);
GO

-- Supports the due-email scan performed by the outbox worker.
CREATE INDEX IX_EmailLogs_status_next_attempt
  ON dbo.EmailLogs(status, next_attempt_at);
GO
