// src/context/ConfirmDialogContext.tsx
'use client';

import React, { createContext, useContext, useState, useCallback, useRef } from 'react';
import ConfirmDialog from '@/components/common/ConfirmDialog';

interface ConfirmOptions {
  title?: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  isDanger?: boolean;
}

interface ConfirmDialogContextType {
  confirm: (options: ConfirmOptions) => Promise<boolean>;
}

const ConfirmDialogContext = createContext<ConfirmDialogContextType | undefined>(undefined);

export function ConfirmDialogProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<{
    open: boolean;
    options: ConfirmOptions;
  }>({
    open: false,
    options: { message: '', title: 'Xác nhận' },
  });

  // Dùng useRef để lưu resolve function của Promise
  const resolveRef = useRef<(value: boolean) => void>(() => {});

  const confirm = useCallback((options: ConfirmOptions) => {
    return new Promise<boolean>((resolve) => {
      setState({
        open: true,
        options: {
          title: options.title || 'Xác nhận hành động',
          confirmLabel: options.confirmLabel || 'Đồng ý',
          cancelLabel: options.cancelLabel || 'Hủy bỏ',
          isDanger: options.isDanger || false,
          ...options,
        },
      });
      resolveRef.current = resolve;
    });
  }, []);

  const handleConfirm = () => {
    setState((prev) => ({ ...prev, open: false }));
    resolveRef.current(true);
  };

  const handleCancel = () => {
    setState((prev) => ({ ...prev, open: false }));
    resolveRef.current(false);
  };

  return (
    <ConfirmDialogContext.Provider value={{ confirm }}>
      {children}
      <ConfirmDialog
        open={state.open}
        title={state.options.title || 'Xác nhận'}
        message={state.options.message}
        confirmLabel={state.options.confirmLabel}
        cancelLabel={state.options.cancelLabel}
        isDanger={state.options.isDanger}
        onConfirm={handleConfirm}
        onCancel={handleCancel}
      />
    </ConfirmDialogContext.Provider>
  );
}

// Hook để dùng ở các component khác
export const useConfirm = () => {
  const context = useContext(ConfirmDialogContext);
  if (!context) throw new Error('useConfirm must be used within a ConfirmDialogProvider');
  return context.confirm;
};