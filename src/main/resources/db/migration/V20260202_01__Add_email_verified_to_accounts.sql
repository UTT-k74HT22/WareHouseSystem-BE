-- Add email_verified column to accounts table
ALTER TABLE accounts 
ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Indicates if user email has been verified';

-- Add index for performance
CREATE INDEX idx_accounts_email_verified ON accounts(email_verified);

-- Add unique constraint for email if not exists
ALTER TABLE accounts 
ADD UNIQUE KEY uk_accounts_email (email);
