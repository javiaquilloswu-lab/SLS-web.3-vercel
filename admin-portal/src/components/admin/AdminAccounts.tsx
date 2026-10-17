import React, { useState, useEffect } from 'react';
import {
  Shield,
  Plus,
  RefreshCw,
  UserCheck,
  X,
  Lock,
  Eye,
  EyeOff,
  AlertTriangle,
  User,
  Mail,
  Building,
  Trash2
} from 'lucide-react';
import { AdminApi, AdminUser, getStoredAdminProfile } from '../../services/adminApi';

interface AdminAccountsProps {
  onShowMessage: (msg: string, isError?: boolean) => void;
}

export const AdminAccounts: React.FC<AdminAccountsProps> = ({ onShowMessage }) => {
  const [accounts, setAccounts] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(true);

  // Modal State
  const [modalOpen, setModalOpen] = useState(false);
  const [fullName, setFullName] = useState('');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [department, setDepartment] = useState('Student Life Office');
  const [role, setRole] = useState<'ADMIN' | 'SUPER_ADMIN'>('ADMIN');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // Super Admin Confirmation
  const [confirmSuperAdminAlert, setConfirmSuperAdminAlert] = useState(false);

  const currentAdmin = getStoredAdminProfile();

  // Deletion State
  const [deletingAcc, setDeletingAcc] = useState<AdminUser | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const handleConfirmDeleteAccount = async () => {
    if (!deletingAcc) return;

    setIsDeleting(true);
    try {
      const res = await AdminApi.deleteAdminAccount(deletingAcc.id);
      if (res.success) {
        onShowMessage(`Administrator account '${deletingAcc.username}' deleted successfully.`);
        setDeletingAcc(null);
        await fetchAccounts();
      } else {
        onShowMessage(res.message || 'Unable to delete administrator account.', true);
      }
    } catch (err: any) {
      onShowMessage('Error deleting administrator: ' + err.message, true);
    } finally {
      setIsDeleting(false);
    }
  };

  const fetchAccounts = async () => {
    setLoading(true);
    try {
      const res = await AdminApi.getAdminAccounts();
      if (res.success && res.data) {
        setAccounts(res.data.accounts || []);
      } else {
        onShowMessage(res.message || 'Unable to retrieve administrator accounts.', true);
      }
    } catch (err: any) {
      onShowMessage('Error fetching administrator directory: ' + err.message, true);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAccounts();
  }, []);

  const handleCreateAccountSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!fullName.trim() || !username.trim() || !email.trim() || !password) {
      onShowMessage('All required fields must be populated.', true);
      return;
    }

    if (password.length < 8) {
      onShowMessage('Password must be at least 8 characters long.', true);
      return;
    }

    if (password !== confirmPassword) {
      onShowMessage('Password and Password Confirmation do not match.', true);
      return;
    }

    if (role === 'SUPER_ADMIN' && !confirmSuperAdminAlert) {
      setConfirmSuperAdminAlert(true);
      return;
    }

    setSubmitting(true);
    try {
      const res = await AdminApi.createAdminAccount({
        full_name: fullName.trim(),
        username: username.trim(),
        email: email.trim(),
        department: department.trim(),
        role,
        password,
        confirm_password: confirmPassword
      });

      if (res.success) {
        onShowMessage(`Administrator account '${username}' created successfully.`);
        setModalOpen(false);
        resetForm();
        await fetchAccounts();
      } else {
        onShowMessage(res.message || 'Failed to create administrator account.', true);
      }
    } catch (err: any) {
      onShowMessage('Error creating administrator: ' + err.message, true);
    } finally {
      setSubmitting(false);
    }
  };

  const resetForm = () => {
    setFullName('');
    setUsername('');
    setEmail('');
    setDepartment('Student Life Office');
    setRole('ADMIN');
    setPassword('');
    setConfirmPassword('');
    setShowPassword(false);
    setConfirmSuperAdminAlert(false);
  };

  return (
    <div className="space-y-6">
      {/* Title & Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-[#17324D] tracking-tight flex items-center gap-2">
            <Shield size={24} className="text-[#17324D]" />
            <span>Administrator Account Directory</span>
          </h1>
          <p className="text-sm text-[#5F6B76]">
            Manage administrative staff accounts, assign authorization roles, and audit access
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => {
              resetForm();
              setModalOpen(true);
            }}
            className="flex items-center gap-1.5 px-3.5 py-2 bg-[#17324D] hover:bg-[#10283E] text-white text-sm font-semibold rounded-lg shadow-sm transition-colors"
          >
            <Plus size={16} />
            <span>Create Administrator</span>
          </button>
          <button
            onClick={fetchAccounts}
            disabled={loading}
            className="flex items-center gap-1.5 px-3 py-2 bg-white border border-[#D9E0E6] hover:bg-slate-50 text-[#17324D] text-sm font-semibold rounded-lg shadow-sm transition-colors"
          >
            <RefreshCw size={15} className={loading ? 'animate-spin' : ''} />
          </button>
        </div>
      </div>

      {/* Account Table */}
      <div className="bg-white rounded-xl border border-[#D9E0E6] shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-[#F5F7FA] border-b border-[#D9E0E6] text-xs font-bold uppercase tracking-wider text-[#5F6B76]">
                <th className="py-3.5 px-4">Administrator</th>
                <th className="py-3.5 px-4">Username & Role</th>
                <th className="py-3.5 px-4">Email & Department</th>
                <th className="py-3.5 px-4">Status</th>
                <th className="py-3.5 px-4">Registered Date</th>
                <th className="py-3.5 px-4 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#D9E0E6] text-sm text-[#17202A]">
              {loading ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-slate-500 text-xs">
                    Loading administrator directory from PostgreSQL...
                  </td>
                </tr>
              ) : accounts.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-slate-500 text-xs">
                    No administrator accounts registered.
                  </td>
                </tr>
              ) : (
                accounts.map((acc) => {
                  const isSuperAdmin = acc.role === 'SUPER_ADMIN';

                  return (
                    <tr key={acc.id} className="hover:bg-slate-50/70 transition-colors">
                      <td className="py-3.5 px-4">
                        <div className="font-semibold text-xs text-[#17324D]">{acc.full_name}</div>
                        <div className="text-[11px] text-[#5F6B76] font-mono">ID: #{acc.id}</div>
                      </td>
                      <td className="py-3.5 px-4">
                        <div className="font-mono text-xs font-bold text-[#17202A]">{acc.username}</div>
                        <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-bold mt-0.5 ${
                          isSuperAdmin
                            ? 'bg-rose-50 text-rose-700 border border-rose-200'
                            : 'bg-blue-50 text-blue-700 border border-blue-200'
                        }`}>
                          {isSuperAdmin ? 'SUPER_ADMIN' : 'STAFF ADMIN'}
                        </span>
                      </td>
                      <td className="py-3.5 px-4">
                        <div className="text-xs font-medium text-[#17202A]">{acc.email}</div>
                        <div className="text-[11px] text-[#5F6B76]">{acc.department || 'Student Life Office'}</div>
                      </td>
                      <td className="py-3.5 px-4">
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-[#2E7D5B] border border-emerald-200">
                          <span className="w-1.5 h-1.5 rounded-full bg-[#2E7D5B]"></span>
                          Active
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-xs text-[#5F6B76] font-mono">
                        {acc.created_at ? new Date(acc.created_at).toLocaleDateString() : 'N/A'}
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        {acc.id !== currentAdmin?.id ? (
                          <button
                            onClick={() => setDeletingAcc(acc)}
                            className="px-2.5 py-1 bg-rose-50 border border-rose-200 hover:bg-rose-100 text-xs font-semibold text-rose-700 rounded shadow-sm transition-colors flex items-center gap-1 ml-auto"
                            title="Delete Administrator Account"
                          >
                            <Trash2 size={13} />
                            <span>Delete</span>
                          </button>
                        ) : (
                          <span className="text-xs text-slate-400 italic">Current User</span>
                        )}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Create Administrator Modal */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl max-w-lg w-full p-6 shadow-2xl border border-[#D9E0E6] space-y-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-start justify-between border-b border-[#D9E0E6] pb-3">
              <div>
                <h3 className="text-base font-bold text-[#17324D]">Create Administrator Account</h3>
                <span className="text-xs text-[#5F6B76]">Assign staff credentials and access privileges</span>
              </div>
              <button
                onClick={() => setModalOpen(false)}
                className="p-1 text-slate-400 hover:text-slate-700 rounded transition-colors"
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleCreateAccountSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-[#17202A] mb-1">Full Name *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Maria Clara Santos"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    className="w-full p-2 bg-[#F5F7FA] border border-[#D9E0E6] rounded-lg text-xs text-[#17202A] focus:outline-none focus:bg-white"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#17202A] mb-1">Username *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. msantos"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    className="w-full p-2 bg-[#F5F7FA] border border-[#D9E0E6] rounded-lg text-xs font-mono text-[#17202A] focus:outline-none focus:bg-white"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-[#17202A] mb-1">Institutional Email *</label>
                  <input
                    type="email"
                    required
                    placeholder="e.g. msantos@swu.phinma.edu.ph"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="w-full p-2 bg-[#F5F7FA] border border-[#D9E0E6] rounded-lg text-xs text-[#17202A] focus:outline-none focus:bg-white"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#17202A] mb-1">Department</label>
                  <input
                    type="text"
                    value={department}
                    onChange={(e) => setDepartment(e.target.value)}
                    className="w-full p-2 bg-[#F5F7FA] border border-[#D9E0E6] rounded-lg text-xs text-[#17202A] focus:outline-none focus:bg-white"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-[#17202A] mb-1">Administrator Role *</label>
                <select
                  value={role}
                  onChange={(e) => {
                    const selected = e.target.value as 'ADMIN' | 'SUPER_ADMIN';
                    setRole(selected);
                    if (selected !== 'SUPER_ADMIN') {
                      setConfirmSuperAdminAlert(false);
                    }
                  }}
                  className="w-full p-2 bg-[#F5F7FA] border border-[#D9E0E6] rounded-lg text-xs font-semibold text-[#17202A] focus:outline-none focus:bg-white"
                >
                  <option value="ADMIN">Staff Admin (Application Review & Support)</option>
                  <option value="SUPER_ADMIN">SUPER_ADMIN (Full Administrative Control)</option>
                </select>
              </div>

              {role === 'SUPER_ADMIN' && (
                <div className="p-3 bg-amber-50 border border-amber-200 rounded-lg text-xs text-amber-800 space-y-1">
                  <div className="font-bold flex items-center gap-1.5 text-amber-900">
                    <AlertTriangle size={15} />
                    <span>Elevated Privilege Warning</span>
                  </div>
                  <p className="text-[11px] leading-relaxed">
                    You are assigning <strong>SUPER_ADMIN</strong> privileges. This grants full control including account deletion, scholarship program destruction, and user management.
                  </p>
                  <label className="flex items-center gap-2 pt-1 font-bold text-amber-900 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={confirmSuperAdminAlert}
                      onChange={(e) => setConfirmSuperAdminAlert(e.target.checked)}
                      className="rounded text-amber-800"
                    />
                    <span>I confirm assigning SUPER_ADMIN role to this user.</span>
                  </label>
                </div>
              )}

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-[#17202A] mb-1">Password *</label>
                  <div className="relative">
                    <input
                      type={showPassword ? 'text' : 'password'}
                      required
                      placeholder="Minimum 8 characters"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      className="w-full p-2 pr-8 bg-[#F5F7FA] border border-[#D9E0E6] rounded-lg text-xs text-[#17202A] focus:outline-none focus:bg-white"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-2 top-2.5 text-slate-400 hover:text-slate-600"
                    >
                      {showPassword ? <EyeOff size={14} /> : <Eye size={14} />}
                    </button>
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-bold text-[#17202A] mb-1">Confirm Password *</label>
                  <input
                    type={showPassword ? 'text' : 'password'}
                    required
                    placeholder="Re-enter password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    className="w-full p-2 bg-[#F5F7FA] border border-[#D9E0E6] rounded-lg text-xs text-[#17202A] focus:outline-none focus:bg-white"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-[#D9E0E6]">
                <button
                  type="button"
                  onClick={() => setModalOpen(false)}
                  disabled={submitting}
                  className="px-4 py-2 text-xs font-semibold text-[#5F6B76] hover:text-[#17202A]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting || (role === 'SUPER_ADMIN' && !confirmSuperAdminAlert)}
                  className="px-4 py-2 bg-[#17324D] hover:bg-[#10283E] disabled:bg-slate-300 text-white text-xs font-bold rounded-lg shadow-sm"
                >
                  {submitting ? 'Creating Account...' : 'Create Account'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Administrator Modal */}
      {deletingAcc && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-2xl border border-rose-200 space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-rose-100 text-rose-600 flex items-center justify-center flex-shrink-0">
                <Trash2 size={20} />
              </div>
              <div>
                <h3 className="text-base font-bold text-[#17324D]">Delete Administrator Account?</h3>
                <span className="text-xs text-rose-600 font-medium">Permanent Destructive Action</span>
              </div>
            </div>

            <p className="text-xs text-[#5F6B76] leading-relaxed">
              Are you sure you want to permanently delete this administrator account? All active sessions for this user will be revoked immediately.
            </p>

            <div className="bg-[#F5F7FA] p-3.5 rounded-lg border border-[#D9E0E6] text-xs space-y-1 font-mono">
              <div><strong className="text-[#5F6B76]">Name:</strong> {deletingAcc.full_name}</div>
              <div><strong className="text-[#5F6B76]">Username:</strong> {deletingAcc.username}</div>
              <div><strong className="text-[#5F6B76]">Email:</strong> {deletingAcc.email}</div>
              <div><strong className="text-[#5F6B76]">Role:</strong> {deletingAcc.role}</div>
            </div>

            <div className="flex justify-end gap-2 pt-3 border-t border-[#D9E0E6]">
              <button
                type="button"
                onClick={() => setDeletingAcc(null)}
                disabled={isDeleting}
                className="px-4 py-2 text-xs font-semibold text-[#5F6B76] hover:text-[#17202A] transition-colors"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmDeleteAccount}
                disabled={isDeleting}
                className="px-4 py-2 bg-rose-600 hover:bg-rose-700 disabled:bg-rose-300 text-white text-xs font-bold rounded-lg shadow-sm transition-colors flex items-center gap-1.5"
              >
                <Trash2 size={14} />
                <span>{isDeleting ? 'Deleting Account...' : 'Confirm Permanent Deletion'}</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
