-- update the status column in stock_transfers table to include the new status 'REJECTED' and PENDING
-- Update by: Dung HD

ALTER TABLE stock_transfers
    MODIFY COLUMN status ENUM(
        'DRAFT',
        'PENDING',
        'COMPLETED',
        'CANCELLED',
        'REJECTED'
) NOT NULL;