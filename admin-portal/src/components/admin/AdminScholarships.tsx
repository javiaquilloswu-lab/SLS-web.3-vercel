import React, { useState, useEffect } from 'react';
import {
  GraduationCap,
  Plus,
  RefreshCw,
  CheckCircle2,
  XCircle,
  Clock,
  X,
  Trash2,
  Edit3
} from 'lucide-react';
import {
  AdminApi,
  AdminScholarshipProgram,
  AdminScholarshipApplication,
  AdminWithdrawalRequest,
  PaginationMeta
} from '../../services/adminApi';
import { AdminPagination } from './AdminPagination';

interface AdminScholarshipsProps {
  onShowMessage: (msg: string, isError?: boolean) => void;
}

interface ReqItemInput {
  name: string;
  description: string;
  allowed_type: string;
  required: boolean;
}

export const AdminScholarships: React.FC<AdminScholarshipsProps> = ({ onShowMessage }) => {
  const [programs, setPrograms] = useState<AdminScholarshipProgram[]>([]);
  const [applications, setApplications] = useState<AdminScholarshipApplication[]>([]);
  const [loading, setLoading] = useState(true);

  // Pagination State for Applications
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(20);
  const [pagination, setPagination] = useState<PaginationMeta | undefined>(undefined);

  // Program creation modal & Requirement Builder state
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [newCode, setNewCode] = useState('');
  const [newTitle, setNewTitle] = useState('');
  const [newDesc, setNewDesc] = useState('');
  const [newAcadYear, setNewAcadYear] = useState('AY 2026–2027');
  const [newDeadline, setNewDeadline] = useState('October 31, 2026');
  const [newStipend, setNewStipend] = useState('₱30,000.00');
  const [reqItems, setReqItems] = useState<ReqItemInput[]>([
    { name: 'Certificate of Grades (COG)', description: 'Certified copy of grades for the preceding academic semester', allowed_type: 'PDF/JPG/PNG', required: true },
    { name: 'Good Moral Certificate', description: 'Current university-issued conduct clearance', allowed_type: 'PDF/JPG/PNG', required: true },
    { name: 'Proof of Income / ITR', description: 'Parents Tax Return or Indigency Certificate', allowed_type: 'PDF', required: true }
  ]);
  const [submittingProgram, setSubmittingProgram] = useState(false);

  // Application evaluation modal
  const [evalApp, setEvalApp] = useState<AdminScholarshipApplication | null>(null);
  const [evalStatus, setEvalStatus] = useState<'Approved' | 'Rejected'>('Approved');
  const [evalNotes, setEvalNotes] = useState('');
  const [submittingEval, setSubmittingEval] = useState(false);

  // Remove Scholar modal state
  const [removingApp, setRemovingApp] = useState<AdminScholarshipApplication | null>(null);
  const [removalReason, setRemovalReason] = useState('');
  const [submittingRemoval, setSubmittingRemoval] = useState(false);

  const handleRemoveScholarSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!removingApp || !removalReason.trim()) return;

    setSubmittingRemoval(true);
    try {
      const res = await AdminApi.removeScholar(removingApp.id, removalReason.trim());
      if (res.success) {
        onShowMessage(`Scholarship for ${removingApp.student_name} removed successfully and student notified.`);
        setRemovingApp(null);
        setRemovalReason('');
        await fetchScholarships();
      } else {
        onShowMessage(res.message || 'Failed to remove scholar.', true);
      }
    } catch (err: any) {
      onShowMessage('Error removing scholar: ' + err.message, true);
    } finally {
      setSubmittingRemoval(false);
    }
  };

  // Withdrawal Requests state & evaluation modal
  const [withdrawalRequests, setWithdrawalRequests] = useState<AdminWithdrawalRequest[]>([]);
  const [activeTab, setActiveTab] = useState<'programs' | 'applications' | 'withdrawals'>('programs');
  const [evalWithdrawalReq, setEvalWithdrawalReq] = useState<AdminWithdrawalRequest | null>(null);
  const [evalWithdrawalDecision, setEvalWithdrawalDecision] = useState<'Approved' | 'Rejected'>('Approved');
  const [evalWithdrawalNotes, setEvalWithdrawalNotes] = useState('');
  const [submittingWithdrawalEval, setSubmittingWithdrawalEval] = useState(false);

  const handleWithdrawalEvalSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!evalWithdrawalReq) return;

    setSubmittingWithdrawalEval(true);
    try {
      const res = await AdminApi.evaluateWithdrawal(evalWithdrawalReq.id, evalWithdrawalDecision, evalWithdrawalNotes);
      if (res.success) {
        onShowMessage(`Withdrawal request marked as ${evalWithdrawalDecision} and student notified.`);
        setEvalWithdrawalReq(null);
        setEvalWithdrawalNotes('');
        await fetchScholarships();
      } else {
        onShowMessage(res.message || 'Failed to evaluate withdrawal request.', true);
      }
    } catch (err: any) {
      onShowMessage('Error evaluating withdrawal request: ' + err.message, true);
    } finally {
      setSubmittingWithdrawalEval(false);
    }
  };

  // Edit Program state
  const [editingProgram, setEditingProgram] = useState<AdminScholarshipProgram | null>(null);
  const [editTitle, setEditTitle] = useState('');
  const [editDesc, setEditDesc] = useState('');
  const [editAcadYear, setEditAcadYear] = useState('');
  const [editDeadline, setEditDeadline] = useState('');
  const [editStipend, setEditStipend] = useState('');
  const [editIsActive, setEditIsActive] = useState(true);
  const [submittingEdit, setSubmittingEdit] = useState(false);

  // Delete / Archive state
  const [deletingProgram, setDeletingProgram] = useState<AdminScholarshipProgram | null>(null);
  const [dependencyConflict, setDependencyConflict] = useState<{ program: AdminScholarshipProgram; message: string } | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const openEditModal = (prog: AdminScholarshipProgram) => {
    setEditingProgram(prog);
    setEditTitle(prog.title);
    setEditDesc(prog.description);
    setEditAcadYear(prog.academic_year);
    setEditDeadline(prog.deadline);
    setEditStipend(prog.stipend_amount);
    setEditIsActive(prog.is_active);
  };

  const handleUpdateProgram = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingProgram) return;
    setSubmittingEdit(true);
    try {
      const res = await AdminApi.updateScholarshipProgram(editingProgram.id, {
        title: editTitle,
        description: editDesc,
        academic_year: editAcadYear,
        deadline: editDeadline,
        stipend_amount: editStipend,
        is_active: editIsActive
      });
      if (res.success) {
        onShowMessage(`Scholarship program '${editTitle}' updated successfully.`);
        setEditingProgram(null);
        await fetchScholarships();
      } else {
        onShowMessage(res.message || 'Failed to update program.', true);
      }
    } catch (err: any) {
      onShowMessage('Error updating scholarship: ' + err.message, true);
    } finally {
      setSubmittingEdit(false);
    }
  };

  const handleDeleteProgram = async (program: AdminScholarshipProgram, archiveOnly = false) => {
    setIsDeleting(true);
    try {
      const res = await AdminApi.deleteScholarshipProgram(program.id, archiveOnly);
      if (res.success) {
        onShowMessage(res.message || (archiveOnly ? 'Program archived successfully.' : 'Program deleted successfully.'));
        setDeletingProgram(null);
        setDependencyConflict(null);
        await fetchScholarships();
      } else {
        if (res.data?.has_dependencies || (res.message && (res.message.includes('attached') || res.message.includes('applications')))) {
          setDeletingProgram(null);
          setDependencyConflict({
            program,
            message: res.message
          });
        } else {
          onShowMessage(res.message || 'Unable to delete program.', true);
        }
      }
    } catch (err: any) {
      onShowMessage('Error deleting scholarship program: ' + err.message, true);
    } finally {
      setIsDeleting(false);
    }
  };

  const addReqItem = () => {
    setReqItems([...reqItems, { name: '', description: '', allowed_type: 'PDF/JPG/PNG', required: true }]);
  };

  const removeReqItem = (index: number) => {
    setReqItems(reqItems.filter((_, i) => i !== index));
  };

  const updateReqItem = (index: number, field: keyof ReqItemInput, value: any) => {
    const updated = [...reqItems];
    updated[index] = { ...updated[index], [field]: value };
    setReqItems(updated);
  };

  const fetchScholarships = async () => {
    setLoading(true);
    try {
      const res = await AdminApi.getScholarships(page, limit);
      if (res.success && res.data) {
        setPrograms(res.data.programs || []);
        setApplications(res.data.applications || []);
        setWithdrawalRequests(res.data.withdrawal_requests || []);
        setPagination(res.data.pagination);
      } else {
        onShowMessage(res.message || 'Unable to query scholarships.', true);
      }
    } catch (err: any) {
      onShowMessage('Error querying scholarships: ' + err.message, true);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchScholarships();
  }, [page, limit]);

  const handleCreateProgram = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmittingProgram(true);
    try {
      const res = await AdminApi.createScholarshipProgram({
        code: newCode,
        title: newTitle,
        description: newDesc,
        requirements: reqItems.filter(r => r.name.trim() !== ''),
        academic_year: newAcadYear,
        deadline: newDeadline,
        stipend_amount: newStipend
      });

      if (res.success) {
        onShowMessage(`Scholarship program '${newTitle}' created in PostgreSQL.`);
        setCreateModalOpen(false);
        setNewCode('');
        setNewTitle('');
        setNewDesc('');
        await fetchScholarships();
      } else {
        onShowMessage(res.message || 'Failed to create program.', true);
      }
    } catch (err: any) {
      onShowMessage('Error creating scholarship: ' + err.message, true);
    } finally {
      setSubmittingProgram(false);
    }
  };

  const handleEvaluateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!evalApp) return;

    setSubmittingEval(true);
    try {
      const res = await AdminApi.updateApplication(evalApp.id, evalStatus, evalNotes);
      if (res.success) {
        onShowMessage(`Application #${evalApp.tracking_ref} marked as ${evalStatus} and student notified.`);
        setEvalApp(null);
        setEvalNotes('');
        await fetchScholarships();
      } else {
        onShowMessage(res.message || 'Failed to evaluate application.', true);
      }
    } catch (err: any) {
      onShowMessage('Error evaluating application: ' + err.message, true);
    } finally {
      setSubmittingEval(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Title & Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-[#17324D] tracking-tight">Scholarship Management Center</h1>
          <p className="text-sm text-[#5F6B76]">
            Manage institutional scholarship grants, renewal deadlines, and bursar evaluations
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => setCreateModalOpen(true)}
            className="flex items-center gap-1.5 px-3.5 py-2 bg-[#17324D] hover:bg-[#10283E] text-white text-sm font-semibold rounded-lg shadow-sm transition-colors"
          >
            <Plus size={16} />
            <span>Create Scholarship</span>
          </button>
          <button
            onClick={fetchScholarships}
            disabled={loading}
            className="flex items-center gap-1.5 px-3 py-2 bg-white border border-[#D9E0E6] hover:bg-slate-50 text-[#17324D] text-sm font-semibold rounded-lg shadow-sm transition-colors"
          >
            <RefreshCw size={15} className={loading ? 'animate-spin' : ''} />
          </button>
        </div>
      </div>

      {/* Switcher Tabs */}
      <div className="border-b border-[#D9E0E6] flex gap-6">
        <button
          onClick={() => setActiveTab('programs')}
          className={`pb-3 text-sm font-bold border-b-2 transition-colors ${
            activeTab === 'programs'
              ? 'border-[#17324D] text-[#17324D]'
              : 'border-transparent text-[#5F6B76] hover:text-[#17202A]'
          }`}
        >
          Active Programs ({programs.length})
        </button>
        <button
          onClick={() => setActiveTab('applications')}
          className={`pb-3 text-sm font-bold border-b-2 transition-colors ${
            activeTab === 'applications'
              ? 'border-[#17324D] text-[#17324D]'
              : 'border-transparent text-[#5F6B76] hover:text-[#17202A]'
          }`}
        >
          Student Applications ({applications.length})
        </button>
        <button
          onClick={() => setActiveTab('withdrawals')}
          className={`pb-3 text-sm font-bold border-b-2 transition-colors flex items-center gap-1.5 ${
            activeTab === 'withdrawals'
              ? 'border-[#17324D] text-[#17324D]'
              : 'border-transparent text-[#5F6B76] hover:text-[#17202A]'
          }`}
        >
          <span>Withdrawal Requests</span>
          <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-rose-100 text-rose-700">
            {withdrawalRequests.filter(r => r.request_status === 'Pending').length}
          </span>
        </button>
      </div>

      {/* Programs Tab */}
      {activeTab === 'programs' && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {loading ? (
            <div className="col-span-3 py-12 text-center text-sm text-[#5F6B76]">
              Loading scholarship programs from PostgreSQL...
            </div>
          ) : programs.length === 0 ? (
            <div className="col-span-3 py-12 text-center text-sm text-[#5F6B76]">
              No active scholarship programs created yet.
            </div>
          ) : (
            programs.map((prog) => (
              <div
                key={prog.id}
                className="bg-white p-5 rounded-xl border border-[#D9E0E6] shadow-sm space-y-3 flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-center justify-between">
                    <span className="px-2 py-0.5 rounded text-[11px] font-mono font-bold bg-slate-100 text-[#17324D] border border-slate-200">
                      {prog.code}
                    </span>
                    <span className={`inline-flex items-center gap-1 text-[11px] font-semibold ${
                      prog.is_expired
                        ? 'text-rose-600'
                        : prog.is_active
                        ? 'text-[#2E7D5B]'
                        : 'text-slate-400'
                    }`}>
                      <span className={`w-1.5 h-1.5 rounded-full ${
                        prog.is_expired
                          ? 'bg-rose-600'
                          : prog.is_active
                          ? 'bg-[#2E7D5B]'
                          : 'bg-slate-400'
                      }`}></span>
                      {prog.is_expired ? 'Expired' : prog.is_active ? 'Active' : 'Inactive'}
                    </span>
                  </div>
                  <h3 className="font-bold text-sm text-[#17324D] mt-2">{prog.title}</h3>
                  <p className="text-xs text-[#5F6B76] mt-1 line-clamp-2">{prog.description}</p>
                </div>

                <div className="pt-3 border-t border-[#D9E0E6] text-xs text-[#5F6B76] space-y-1">
                  <div className="flex justify-between">
                    <span>Academic Year:</span>
                    <span className="font-medium text-[#17202A]">{prog.academic_year}</span>
                  </div>
                  <div className="flex justify-between">
                    <span>Deadline:</span>
                    <span className="font-medium text-[#17202A]">{prog.deadline}</span>
                  </div>
                  <div className="flex justify-between">
                    <span>Grant Stipend:</span>
                    <span className="font-bold text-[#2E7D5B]">{prog.stipend_amount}</span>
                  </div>
                </div>

                <div className="pt-3 border-t border-[#D9E0E6] flex items-center justify-between">
                  <span className="text-[11px] text-[#5F6B76] font-mono">ID: #{prog.id}</span>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => openEditModal(prog)}
                      className="px-3 py-1 bg-white border border-[#D9E0E6] hover:bg-slate-50 text-xs font-semibold text-[#17324D] rounded shadow-sm transition-colors flex items-center gap-1"
                    >
                      <Edit3 size={13} />
                      <span>Edit</span>
                    </button>
                    <button
                      onClick={() => setDeletingProgram(prog)}
                      className="px-3 py-1 bg-rose-50 border border-rose-200 hover:bg-rose-100 text-xs font-semibold text-rose-700 rounded shadow-sm transition-colors flex items-center gap-1"
                    >
                      <Trash2 size={13} />
                      <span>Delete</span>
                    </button>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {/* Applications Tab */}
      {activeTab === 'applications' && (
        <div className="bg-white rounded-xl border border-[#D9E0E6] shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-[#F5F7FA] border-b border-[#D9E0E6] text-xs font-bold uppercase tracking-wider text-[#5F6B76]">
                  <th className="py-3.5 px-4">Tracking Ref</th>
                  <th className="py-3.5 px-4">Applicant Student</th>
                  <th className="py-3.5 px-4">Program Applied</th>
                  <th className="py-3.5 px-4">Type</th>
                  <th className="py-3.5 px-4">Status</th>
                  <th className="py-3.5 px-4 text-right">Evaluation</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#D9E0E6] text-sm text-[#17202A]">
                {loading ? (
                  <tr>
                    <td colSpan={6} className="py-12 text-center text-slate-500">
                      Querying applications from PostgreSQL...
                    </td>
                  </tr>
                ) : applications.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="py-12 text-center text-slate-500">
                      No scholarship applications submitted yet.
                    </td>
                  </tr>
                ) : (
                  applications.map((app) => (
                    <tr key={app.id} className="hover:bg-slate-50/70 transition-colors">
                      <td className="py-3.5 px-4 font-mono text-xs font-bold text-[#17324D]">
                        {app.tracking_ref}
                      </td>
                      <td className="py-3.5 px-4">
                        <div className="font-semibold text-xs text-[#17202A]">{app.student_name || 'Student'}</div>
                        <div className="text-[11px] text-[#5F6B76]">{app.student_id} • {app.course}</div>
                      </td>
                      <td className="py-3.5 px-4 text-xs font-medium text-[#17202A]">
                        {app.program_name}
                      </td>
                      <td className="py-3.5 px-4 text-xs text-[#5F6B76]">
                        {app.application_type}
                      </td>
                      <td className="py-3.5 px-4">
                        <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-50 text-[#B7791F] border border-amber-200">
                          <span className="w-1.5 h-1.5 rounded-full bg-[#B7791F]"></span>
                          {app.status}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => {
                              setEvalApp(app);
                              setEvalStatus('Approved');
                              setEvalNotes(app.admin_notes || '');
                            }}
                            className="px-3 py-1 bg-white border border-[#D9E0E6] hover:bg-slate-100 text-xs font-semibold text-[#17324D] rounded shadow-sm transition-colors"
                          >
                            Evaluate
                          </button>
                          {app.status && app.status.toLowerCase() === 'approved' && (
                            <button
                              onClick={() => {
                                setRemovingApp(app);
                                setRemovalReason('');
                              }}
                              className="px-2.5 py-1 bg-rose-50 border border-rose-200 hover:bg-rose-100 text-xs font-semibold text-rose-700 rounded shadow-sm transition-colors"
                            >
                              Remove Scholar
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
          <AdminPagination
            pagination={pagination}
            onPageChange={setPage}
            onLimitChange={(l) => { setLimit(l); setPage(1); }}
            loading={loading}
          />
        </div>
      )}

      {/* Withdrawal Requests Tab */}
      {activeTab === 'withdrawals' && (
        <div className="bg-white rounded-xl border border-[#D9E0E6] shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-[#F5F7FA] border-b border-[#D9E0E6] text-xs font-bold uppercase tracking-wider text-[#5F6B76]">
                  <th className="py-3.5 px-4">Applicant Student</th>
                  <th className="py-3.5 px-4">Scholarship Program</th>
                  <th className="py-3.5 px-4">Withdrawal Reason</th>
                  <th className="py-3.5 px-4">Status</th>
                  <th className="py-3.5 px-4 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#D9E0E6] text-sm text-[#17202A]">
                {loading ? (
                  <tr>
                    <td colSpan={5} className="py-12 text-center text-slate-500">
                      Querying withdrawal requests from PostgreSQL...
                    </td>
                  </tr>
                ) : withdrawalRequests.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="py-12 text-center text-slate-500">
                      No scholarship withdrawal requests found.
                    </td>
                  </tr>
                ) : (
                  withdrawalRequests.map((req) => {
                    const isPending = req.request_status === 'Pending';
                    const statusColor = req.request_status === 'Approved' ? 'bg-emerald-50 text-[#2E7D5B] border-emerald-200' : (req.request_status === 'Rejected' ? 'bg-rose-50 text-rose-700 border-rose-200' : 'bg-amber-50 text-[#B7791F] border-amber-200');

                    return (
                      <tr key={req.id} className="hover:bg-slate-50/70 transition-colors">
                        <td className="py-3.5 px-4">
                          <div className="font-semibold text-xs text-[#17202A]">{req.student_name || 'Student'}</div>
                          <div className="text-[11px] text-[#5F6B76] font-mono">{req.student_id} • {req.course}</div>
                        </td>
                        <td className="py-3.5 px-4">
                          <div className="font-semibold text-xs text-[#17202A]">{req.program_name}</div>
                          <div className="text-[11px] text-[#5F6B76] font-mono">Ref: {req.tracking_ref}</div>
                        </td>
                        <td className="py-3.5 px-4 text-xs text-[#5F6B76] max-w-xs truncate" title={req.withdrawal_reason}>
                          {req.withdrawal_reason}
                        </td>
                        <td className="py-3.5 px-4">
                          <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold border ${statusColor}`}>
                            <span className="w-1.5 h-1.5 rounded-full bg-current"></span>
                            {req.request_status}
                          </span>
                        </td>
                        <td className="py-3.5 px-4 text-right">
                          {isPending ? (
                            <button
                              onClick={() => {
                                setEvalWithdrawalReq(req);
                                setEvalWithdrawalDecision('Approved');
                                setEvalWithdrawalNotes('');
                              }}
                              className="px-3 py-1 bg-[#17324D] hover:bg-[#10283E] text-white text-xs font-semibold rounded shadow-sm transition-colors"
                            >
                              Review Request
                            </button>
                          ) : (
                            <span className="text-xs text-[#5F6B76]">Reviewed ({req.reviewed_by || 'Admin'})</span>
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
      )}

      {/* Create Program Modal */}
      {createModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-2xl border border-[#D9E0E6] space-y-4">
            <div className="flex items-start justify-between border-b border-[#D9E0E6] pb-3">
              <div>
                <h3 className="text-base font-bold text-[#17324D]">Create Scholarship Program</h3>
                <span className="text-xs text-[#5F6B76]">Adds to master database catalog</span>
              </div>
              <button
                onClick={() => setCreateModalOpen(false)}
                className="p-1 text-slate-400 hover:text-slate-700 rounded transition-colors"
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleCreateProgram} className="space-y-3">
              <div>
                <label className="block text-xs font-bold text-[#17202A] mb-1">Program Code</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. CHED-CMSP-2026"
                  value={newCode}
                  onChange={(e) => setNewCode(e.target.value)}
                  className="w-full p-2 bg-[#F5F7FA] border border-[#D9E0E6] rounded-lg text-xs font-mono text-[#17202A] focus:outline-none focus:bg-white"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-[#17202A] mb-1">Scholarship Title</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. CHED Academic Excellence Merit Award"
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  className="w-full p-2 bg-[#F5F7FA] border border-[#D9E0E6] rounded-lg text-xs text-[#17202A] focus:outline-none focus:bg-white"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-[#17202A] mb-1">Description / Eligibility</label>
                <textarea
                  rows={2}
                  placeholder="e.g. Open to 3rd & 4th year collegiate scholars with minimum 1.35 GWA..."
                  value={newDesc}
                  onChange={(e) => setNewDesc(e.target.value)}
                  className="w-full p-2 bg-[#F5F7FA] border border-[#D9E0E6] rounded-lg text-xs text-[#17202A] focus:outline-none focus:bg-white"
                />
              </div>

              {/* Dynamic Requirement Builder Section */}
              <div className="pt-2 border-t border-[#D9E0E6]">
                <div className="flex items-center justify-between mb-2">
                  <label className="block text-xs font-bold text-[#17324D]">Program Requirements Builder</label>
                  <button
                    type="button"
                    onClick={addReqItem}
                    className="text-[11px] font-bold text-[#17324D] hover:underline flex items-center gap-1"
                  >
                    <Plus size={12} />
                    <span>Add Requirement</span>
                  </button>
                </div>

                <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
                  {reqItems.map((req, idx) => (
                    <div key={idx} className="p-2.5 bg-[#F5F7FA] border border-[#D9E0E6] rounded-lg text-xs space-y-2">
                      <div className="flex items-center justify-between gap-2">
                        <input
                          type="text"
                          required
                          placeholder="Requirement Name (e.g. COG)"
                          value={req.name}
                          onChange={(e) => updateReqItem(idx, 'name', e.target.value)}
                          className="flex-1 p-1 bg-white border border-[#D9E0E6] rounded text-xs font-semibold text-[#17202A]"
                        />
                        {reqItems.length > 1 && (
                          <button
                            type="button"
                            onClick={() => removeReqItem(idx)}
                            className="text-rose-600 hover:text-rose-800 text-[11px] font-semibold"
                          >
                            Remove
                          </button>
                        )}
                      </div>

                      <input
                        type="text"
                        placeholder="Description / Instructions"
                        value={req.description}
                        onChange={(e) => updateReqItem(idx, 'description', e.target.value)}
                        className="w-full p-1 bg-white border border-[#D9E0E6] rounded text-xs text-[#5F6B76]"
                      />

                      <div className="flex items-center justify-between gap-2 text-[11px]">
                        <div className="flex items-center gap-1.5">
                          <span className="text-[#5F6B76] font-medium">Format:</span>
                          <select
                            value={req.allowed_type}
                            onChange={(e) => updateReqItem(idx, 'allowed_type', e.target.value)}
                            className="p-1 bg-white border border-[#D9E0E6] rounded font-semibold text-[#17202A]"
                          >
                            <option value="PDF/JPG/PNG">PDF / JPG / PNG</option>
                            <option value="PDF">PDF Only</option>
                            <option value="JPG/PNG">Images (JPG/PNG)</option>
                            <option value="PDF/DOCX">PDF / DOCX</option>
                          </select>
                        </div>

                        <label className="flex items-center gap-1 cursor-pointer font-bold text-[#17324D]">
                          <input
                            type="checkbox"
                            checked={req.required}
                            onChange={(e) => updateReqItem(idx, 'required', e.target.checked)}
                            className="rounded text-[#17324D]"
                          />
                          <span>Mandatory</span>
                        </label>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-[#17202A] mb-1">Academic Cycle</label>
                  <input
                    type="text"
                    value={newAcadYear}
                    onChange={(e) => setNewAcadYear(e.target.value)}
                    className="w-full p-2 bg-[#F5F7FA] border border-[#D9E0E6] rounded-lg text-xs text-[#17202A]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#17202A] mb-1">Deadline Date</label>
                  <input
                    type="text"
                    value={newDeadline}
                    onChange={(e) => setNewDeadline(e.target.value)}
                    className="w-full p-2 bg-[#F5F7FA] border border-[#D9E0E6] rounded-lg text-xs text-[#17202A]"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-[#17202A] mb-1">Grant Stipend Amount</label>
                <input
                  type="text"
                  value={newStipend}
                  onChange={(e) => setNewStipend(e.target.value)}
                  className="w-full p-2 bg-[#F5F7FA] border border-[#D9E0E6] rounded-lg text-xs font-bold text-[#2E7D5B]"
                />
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-[#D9E0E6]">
                <button
                  type="button"
                  onClick={() => setCreateModalOpen(false)}
                  disabled={submittingProgram}
                  className="px-3 py-1.5 text-xs font-semibold text-[#5F6B76] hover:text-[#17202A]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingProgram}
                  className="px-4 py-2 bg-[#17324D] hover:bg-[#10283E] text-white text-xs font-semibold rounded-lg shadow-sm"
                >
                  {submittingProgram ? 'Saving...' : 'Create Program'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Evaluate Application Modal */}
      {evalApp && (
        <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-2xl border border-[#D9E0E6] space-y-4">
            <div className="flex items-start justify-between border-b border-[#D9E0E6] pb-3">
              <div>
                <h3 className="text-base font-bold text-[#17324D]">Evaluate Application</h3>
                <span className="text-xs text-[#5F6B76]">Reference: {evalApp.tracking_ref}</span>
              </div>
              <button
                onClick={() => setEvalApp(null)}
                className="p-1 text-slate-400 hover:text-slate-700 rounded transition-colors"
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleEvaluateSubmit} className="space-y-4">
              <div className="bg-[#F5F7FA] p-3 rounded-lg border border-[#D9E0E6] text-xs space-y-1">
                <div>Applicant: <strong>{evalApp.student_name}</strong> ({evalApp.student_id})</div>
                <div>Program: <strong>{evalApp.program_name}</strong></div>
              </div>

              <div>
                <label className="block text-xs font-bold text-[#17202A] mb-1.5">Decision</label>
                <div className="grid grid-cols-2 gap-3">
                  <button
                    type="button"
                    onClick={() => setEvalStatus('Approved')}
                    className={`py-2 px-3 rounded-lg border text-xs font-bold flex items-center justify-center gap-1.5 ${
                      evalStatus === 'Approved'
                        ? 'bg-emerald-50 border-emerald-500 text-[#2E7D5B]'
                        : 'border-[#D9E0E6] text-[#5F6B76] hover:bg-slate-50'
                    }`}
                  >
                    <CheckCircle2 size={16} />
                    <span>Approve Application</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setEvalStatus('Rejected')}
                    className={`py-2 px-3 rounded-lg border text-xs font-bold flex items-center justify-center gap-1.5 ${
                      evalStatus === 'Rejected'
                        ? 'bg-rose-50 border-rose-500 text-[#B42318]'
                        : 'border-[#D9E0E6] text-[#5F6B76] hover:bg-slate-50'
                    }`}
                  >
                    <XCircle size={16} />
                    <span>Reject Application</span>
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-[#17202A] mb-1">
                  Bursar Evaluation Notes (Saved to PostgreSQL & Dispatched to Student)
                </label>
                <textarea
                  rows={3}
                  value={evalNotes}
                  onChange={(e) => setEvalNotes(e.target.value)}
                  placeholder="e.g. Qualification requirements met. Endorsed for stipend release..."
                  className="w-full p-2.5 bg-[#F5F7FA] border border-[#D9E0E6] focus:border-[#17324D] rounded-lg text-xs text-[#17202A] focus:outline-none focus:bg-white"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-[#D9E0E6]">
                <button
                  type="button"
                  onClick={() => setEvalApp(null)}
                  disabled={submittingEval}
                  className="px-3.5 py-2 text-xs font-semibold text-[#5F6B76] hover:text-[#17202A]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingEval}
                  className="px-4 py-2 bg-[#17324D] hover:bg-[#10283E] text-white text-xs font-semibold rounded-lg shadow-sm"
                >
                  {submittingEval ? 'Saving...' : 'Save & Dispatch Decision'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Program Modal */}
      {editingProgram && (
        <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl max-w-lg w-full p-6 shadow-2xl border border-[#D9E0E6] space-y-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-start justify-between border-b border-[#D9E0E6] pb-3">
              <div>
                <h3 className="text-lg font-bold text-[#17324D]">Edit Scholarship Program</h3>
                <span className="text-xs text-[#5F6B76] font-mono">Code: {editingProgram.code} (ID: #{editingProgram.id})</span>
              </div>
              <button
                onClick={() => setEditingProgram(null)}
                className="p-1 text-slate-400 hover:text-slate-700 rounded transition-colors"
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleUpdateProgram} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-[#17202A] mb-1">Program Title *</label>
                <input
                  type="text"
                  required
                  value={editTitle}
                  onChange={(e) => setEditTitle(e.target.value)}
                  className="w-full p-2 bg-[#F5F7FA] border border-[#D9E0E6] rounded-lg text-xs text-[#17202A] focus:outline-none focus:bg-white"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-[#17202A] mb-1">Description / Eligibility</label>
                <textarea
                  rows={2}
                  value={editDesc}
                  onChange={(e) => setEditDesc(e.target.value)}
                  className="w-full p-2 bg-[#F5F7FA] border border-[#D9E0E6] rounded-lg text-xs text-[#17202A] focus:outline-none focus:bg-white"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-[#17202A] mb-1">Academic Cycle</label>
                  <input
                    type="text"
                    value={editAcadYear}
                    onChange={(e) => setEditAcadYear(e.target.value)}
                    className="w-full p-2 bg-[#F5F7FA] border border-[#D9E0E6] rounded-lg text-xs text-[#17202A]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#17202A] mb-1">Deadline Date</label>
                  <input
                    type="text"
                    value={editDeadline}
                    onChange={(e) => setEditDeadline(e.target.value)}
                    className="w-full p-2 bg-[#F5F7FA] border border-[#D9E0E6] rounded-lg text-xs text-[#17202A]"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3 items-center">
                <div>
                  <label className="block text-xs font-bold text-[#17202A] mb-1">Grant Stipend Amount</label>
                  <input
                    type="text"
                    value={editStipend}
                    onChange={(e) => setEditStipend(e.target.value)}
                    className="w-full p-2 bg-[#F5F7FA] border border-[#D9E0E6] rounded-lg text-xs font-bold text-[#2E7D5B]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#17202A] mb-1">Status</label>
                  <label className="flex items-center gap-2 mt-2 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={editIsActive}
                      onChange={(e) => setEditIsActive(e.target.checked)}
                      className="rounded text-[#17324D]"
                    />
                    <span className="text-xs font-semibold text-[#17324D]">Active & Open for Applications</span>
                  </label>
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-[#D9E0E6]">
                <button
                  type="button"
                  onClick={() => setEditingProgram(null)}
                  disabled={submittingEdit}
                  className="px-3 py-1.5 text-xs font-semibold text-[#5F6B76] hover:text-[#17202A]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingEdit}
                  className="px-4 py-2 bg-[#17324D] hover:bg-[#10283E] text-white text-xs font-semibold rounded-lg shadow-sm"
                >
                  {submittingEdit ? 'Saving...' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {deletingProgram && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-2xl border border-rose-200 space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-rose-100 text-rose-600 flex items-center justify-center flex-shrink-0">
                <Trash2 size={20} />
              </div>
              <div>
                <h3 className="text-base font-bold text-[#17324D]">Permanently Delete Program?</h3>
                <span className="text-xs text-rose-600 font-medium">Destructive Action</span>
              </div>
            </div>

            <p className="text-xs text-[#5F6B76] leading-relaxed">
              Are you sure you want to permanently delete this scholarship program? If student applications are attached, deletion will be blocked and archival will be recommended.
            </p>

            <div className="bg-[#F5F7FA] p-3.5 rounded-lg border border-[#D9E0E6] text-xs space-y-1 font-mono">
              <div><strong className="text-[#5F6B76]">Code:</strong> {deletingProgram.code}</div>
              <div><strong className="text-[#5F6B76]">Title:</strong> {deletingProgram.title}</div>
            </div>

            <div className="flex justify-end gap-2 pt-3 border-t border-[#D9E0E6]">
              <button
                type="button"
                onClick={() => setDeletingProgram(null)}
                disabled={isDeleting}
                className="px-4 py-2 text-xs font-semibold text-[#5F6B76] hover:text-[#17202A]"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => handleDeleteProgram(deletingProgram, false)}
                disabled={isDeleting}
                className="px-4 py-2 bg-rose-600 hover:bg-rose-700 disabled:bg-rose-300 text-white text-xs font-bold rounded-lg shadow-sm"
              >
                {isDeleting ? 'Processing...' : 'Confirm Permanent Deletion'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Dependency Conflict / Archive Modal */}
      {dependencyConflict && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-2xl border border-amber-200 space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-amber-100 text-amber-700 flex items-center justify-center flex-shrink-0">
                <Clock size={20} />
              </div>
              <div>
                <h3 className="text-base font-bold text-[#17324D]">Deletion Blocked (Applications Attached)</h3>
                <span className="text-xs text-amber-700 font-medium">Historical Records Protected</span>
              </div>
            </div>

            <p className="text-xs text-[#5F6B76] leading-relaxed">
              {dependencyConflict.message}
            </p>

            <div className="bg-[#F5F7FA] p-3.5 rounded-lg border border-[#D9E0E6] text-xs space-y-1 font-mono">
              <div><strong className="text-[#5F6B76]">Program:</strong> {dependencyConflict.program.title} ({dependencyConflict.program.code})</div>
            </div>

            <div className="flex justify-end gap-2 pt-3 border-t border-[#D9E0E6]">
              <button
                type="button"
                onClick={() => setDependencyConflict(null)}
                disabled={isDeleting}
                className="px-4 py-2 text-xs font-semibold text-[#5F6B76] hover:text-[#17202A]"
              >
                Close
              </button>
              <button
                type="button"
                onClick={() => handleDeleteProgram(dependencyConflict.program, true)}
                disabled={isDeleting}
                className="px-4 py-2 bg-[#17324D] hover:bg-[#10283E] disabled:bg-slate-300 text-white text-xs font-bold rounded-lg shadow-sm"
              >
                {isDeleting ? 'Archiving...' : 'Archive Program (Set Inactive)'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Remove Scholar Modal */}
      {removingApp && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-2xl border border-rose-200 space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-rose-100 text-rose-600 flex items-center justify-center flex-shrink-0">
                <Trash2 size={20} />
              </div>
              <div>
                <h3 className="text-base font-bold text-[#17324D]">Remove Active Scholar?</h3>
                <span className="text-xs text-rose-600 font-medium">Revocation of Award</span>
              </div>
            </div>

            <p className="text-xs text-[#5F6B76] leading-relaxed">
              Are you sure you want to remove this student from their active scholarship grant? This will mark the application as Removed and notify the student.
            </p>

            <div className="bg-[#F5F7FA] p-3.5 rounded-lg border border-[#D9E0E6] text-xs space-y-1 font-mono">
              <div><strong className="text-[#5F6B76]">Student:</strong> {removingApp.student_name} ({removingApp.student_id})</div>
              <div><strong className="text-[#5F6B76]">Program:</strong> {removingApp.program_name}</div>
            </div>

            <form onSubmit={handleRemoveScholarSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-[#17202A] mb-1">Reason for Removal *</label>
                <textarea
                  rows={3}
                  required
                  value={removalReason}
                  onChange={(e) => setRemovalReason(e.target.value)}
                  placeholder="e.g. Failure to maintain required GWA threshold..."
                  className="w-full p-2.5 bg-[#F5F7FA] border border-[#D9E0E6] focus:border-[#17324D] rounded-lg text-xs text-[#17202A] focus:outline-none focus:bg-white"
                />
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-[#D9E0E6]">
                <button
                  type="button"
                  onClick={() => setRemovingApp(null)}
                  disabled={submittingRemoval}
                  className="px-4 py-2 text-xs font-semibold text-[#5F6B76] hover:text-[#17202A]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingRemoval || !removalReason.trim()}
                  className="px-4 py-2 bg-rose-600 hover:bg-rose-700 disabled:bg-rose-300 text-white text-xs font-bold rounded-lg shadow-sm"
                >
                  {submittingRemoval ? 'Removing...' : 'Confirm Scholar Removal'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Review Withdrawal Request Modal */}
      {evalWithdrawalReq && (
        <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-2xl border border-[#D9E0E6] space-y-4">
            <div className="flex items-start justify-between border-b border-[#D9E0E6] pb-3">
              <div>
                <h3 className="text-base font-bold text-[#17324D]">Review Withdrawal Request</h3>
                <span className="text-xs text-[#5F6B76] font-mono">Ref: {evalWithdrawalReq.tracking_ref}</span>
              </div>
              <button
                onClick={() => setEvalWithdrawalReq(null)}
                className="p-1 text-slate-400 hover:text-slate-700 rounded transition-colors"
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleWithdrawalEvalSubmit} className="space-y-4">
              <div className="bg-[#F5F7FA] p-3 rounded-lg border border-[#D9E0E6] text-xs space-y-1">
                <div>Applicant: <strong>{evalWithdrawalReq.student_name}</strong> ({evalWithdrawalReq.student_id})</div>
                <div>Program: <strong>{evalWithdrawalReq.program_name}</strong></div>
                <div className="pt-2 text-[#17202A]">
                  <strong>Withdrawal Reason:</strong>
                  <p className="mt-1 p-2 bg-white rounded border border-[#D9E0E6] text-[#5F6B76] italic">{evalWithdrawalReq.withdrawal_reason}</p>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-[#17202A] mb-1.5">Decision</label>
                <div className="grid grid-cols-2 gap-3">
                  <button
                    type="button"
                    onClick={() => setEvalWithdrawalDecision('Approved')}
                    className={`py-2 px-3 rounded-lg border text-xs font-bold flex items-center justify-center gap-1.5 transition-colors ${
                      evalWithdrawalDecision === 'Approved'
                        ? 'bg-rose-50 border-rose-500 text-rose-700'
                        : 'border-[#D9E0E6] text-[#5F6B76] hover:bg-slate-50'
                    }`}
                  >
                    <CheckCircle2 size={16} />
                    <span>Approve Withdrawal</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setEvalWithdrawalDecision('Rejected')}
                    className={`py-2 px-3 rounded-lg border text-xs font-bold flex items-center justify-center gap-1.5 transition-colors ${
                      evalWithdrawalDecision === 'Rejected'
                        ? 'bg-emerald-50 border-emerald-500 text-[#2E7D5B]'
                        : 'border-[#D9E0E6] text-[#5F6B76] hover:bg-slate-50'
                    }`}
                  >
                    <XCircle size={16} />
                    <span>Reject Withdrawal</span>
                  </button>
                </div>
                <p className="text-[11px] text-[#5F6B76] mt-1.5">
                  {evalWithdrawalDecision === 'Approved' ? 'Approving transitions the scholarship to Withdrawn.' : 'Rejecting leaves the scholarship active.'}
                </p>
              </div>

              <div>
                <label className="block text-xs font-bold text-[#17202A] mb-1">Staff Review Notes / Reason</label>
                <textarea
                  rows={3}
                  value={evalWithdrawalNotes}
                  onChange={(e) => setEvalWithdrawalNotes(e.target.value)}
                  placeholder="e.g. Exit interview completed, request acknowledged..."
                  className="w-full p-2.5 bg-[#F5F7FA] border border-[#D9E0E6] focus:border-[#17324D] rounded-lg text-xs text-[#17202A] focus:outline-none focus:bg-white"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-[#D9E0E6]">
                <button
                  type="button"
                  onClick={() => setEvalWithdrawalReq(null)}
                  disabled={submittingWithdrawalEval}
                  className="px-3.5 py-2 text-xs font-semibold text-[#5F6B76] hover:text-[#17202A]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingWithdrawalEval}
                  className="px-4 py-2 bg-[#17324D] hover:bg-[#10283E] text-white text-xs font-semibold rounded-lg shadow-sm"
                >
                  {submittingWithdrawalEval ? 'Saving...' : 'Submit Decision'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
