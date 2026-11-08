(() => {
  const modal = document.getElementById('login-modal');
  const openButton = document.getElementById('open-login');
  const closeButton = document.getElementById('close-login');
  const loginError = document.getElementById('login-error');
  let previousFocus;

  function openModal() {
    previousFocus = document.activeElement;
    modal.hidden = false;
    document.body.classList.add('modal-open');
    document.getElementById('username').focus();
  }
  function closeModal() {
    modal.hidden = true;
    document.body.classList.remove('modal-open');
    if (previousFocus) previousFocus.focus();
  }
  openButton.addEventListener('click', openModal);
  closeButton.addEventListener('click', closeModal);
  modal.addEventListener('click', event => { if (event.target === modal) closeModal(); });
  document.addEventListener('keydown', event => {
    if (modal.hidden) return;
    if (event.key === 'Escape') closeModal();
    if (event.key === 'Tab') {
      const focusable = [...modal.querySelectorAll('button,input')].filter(item => !item.disabled);
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
    }
  });
  if (loginError.textContent.trim()) openModal();

  const form = document.getElementById('appointment-form');
  const doctor = document.getElementById('doctorId');
  const date = document.getElementById('appointmentDate');
  const shift = document.getElementById('shift');
  const slot = document.getElementById('slotId');
  const help = document.getElementById('slot-help');
  const savedSlot = slot.dataset.selected;
  const initialContext = `${doctor.value}|${date.value}|${shift.value}`;
  let activeRequest;
  let requestVersion = 0;
  async function loadSlots() {
    const version = ++requestVersion;
    if (activeRequest) activeRequest.abort();
    slot.replaceChildren(new Option('Đang tải khung giờ...', ''));
    slot.disabled = true;
    help.textContent = '';
    if (!doctor.value || !date.value || !shift.value) {
      slot.replaceChildren(new Option('Chọn bác sĩ, ngày và ca khám trước', ''));
      return;
    }
    const selectedContext = `${doctor.value}|${date.value}|${shift.value}`;
    activeRequest = new AbortController();
    try {
      const response = await fetch(`${window.HOSPITAL_CONTEXT}/slots?doctorId=${encodeURIComponent(doctor.value)}&date=${encodeURIComponent(date.value)}&shift=${encodeURIComponent(shift.value)}`, { signal: activeRequest.signal });
      if (!response.ok) throw new Error('Không tải được khung giờ. Vui lòng thử lại.');
      const slots = await response.json();
      if (version !== requestVersion) return;
      slot.replaceChildren(new Option(slots.length ? 'Chọn khung giờ' : 'Không có khung giờ khả dụng', ''));
      slots.forEach(item => {
        const option = new Option(item.label, item.id);
        if (selectedContext === initialContext && savedSlot === String(item.id)) option.selected = true;
        slot.add(option);
      });
      slot.disabled = slots.length === 0;
      if (slots.length) help.textContent = '';
      else if (response.headers.get('X-Slot-Availability') === 'NOT_PUBLISHED') help.textContent = 'Bác sĩ chưa công bố lịch cho ngày và ca này. Vui lòng chọn ca hoặc ngày khác.';
      else help.textContent = 'Lịch đã công bố nhưng hiện không còn khung giờ trống hoặc bác sĩ không nhận khám trong ca này. Vui lòng chọn ca hoặc ngày khác.';
    } catch (error) {
      if (error.name === 'AbortError' || version !== requestVersion) return;
      slot.replaceChildren(new Option('Không tải được khung giờ', ''));
      help.textContent = error.message || 'Không tải được khung giờ. Vui lòng thử lại.';
    }
  }
  doctor.addEventListener('change', loadSlots);
  date.addEventListener('change', loadSlots);
  shift.addEventListener('change', loadSlots);
  if (doctor.value && date.value && shift.value) loadSlots();
  form.addEventListener('submit', event => {
    if (!form.reportValidity() || slot.disabled || !slot.value) {
      event.preventDefault();
      if (!slot.value) help.textContent = 'Vui lòng chọn khung giờ còn trống trước khi gửi.';
      return;
    }
    const button = document.getElementById('submit-appointment');
    button.disabled = true;
    button.textContent = 'Đang xử lý...';
    document.getElementById('appointment-status').textContent = 'Đang lưu thông tin đặt lịch.';
  });
})();
