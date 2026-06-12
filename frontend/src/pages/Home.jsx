import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { useNavigate, Link } from 'react-router-dom'
import api from '../api/axios'
import styles from './Home.module.css'

export default function Home() {
  const { user, login, signup, logout, deleteAccount } = useAuth()
  const navigate = useNavigate()

  // 상태 변수들 - 기본 화면은 입력 화면 (비로그인 사용자도 입력 가능)
  const [step, setStep] = useState('input') // input, loading, result
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [loadingTextIndex, setLoadingTextIndex] = useState(0)
  const [apiError, setApiError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  // 로그인/회원가입 모달 (저장 시 비로그인 상태일 때 표시)
  const [showAuthModal, setShowAuthModal] = useState(false)
  const [authMode, setAuthMode] = useState('login') // login, signup
  const [authForm, setAuthForm] = useState({ email: '', password: '', nickname: '' })
  const [authError, setAuthError] = useState('')
  const [authLoading, setAuthLoading] = useState(false)

  // 사주 입력 폼 상태
  const [form, setForm] = useState({
    name: '',
    gender: 'MALE',
    calendarType: 'SOLAR',
    birthYear: '1995',
    birthMonth: '5',
    birthDay: '5',
    birthHour: '12',
    birthMinute: '00',
    noTime: false,
    birthPlace: '서울'
  })

  // 백엔드에서 받아온 사주 및 캐릭터 정보
  const [sajuResult, setSajuResult] = useState(null)
  const [lastPayload, setLastPayload] = useState(null)
  const [isSaved, setIsSaved] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [saveError, setSaveError] = useState('')

  // 신비로운 로딩 문구들
  const loadingTexts = [
    "🌠 천체의 궤도와 별자리의 배열을 추적하는 중...",
    "☯️ 생년월일의 오행(목, 화, 토, 금, 수) 균형을 연산하는 중...",
    "📜 명리학의 만세력 데이터를 분석하여 운명의 실타래를 짜는 중...",
    "🛡️ 당신의 운명에 조화로운 영웅의 클래스를 각성하는 중...",
    "⚔️ 모험을 위한 운명의 캐릭터 시트를 완성하고 있습니다!"
  ]

  // 로딩 단계 텍스트 롤링 효과
  useEffect(() => {
    let interval;
    if (step === 'loading') {
      interval = setInterval(() => {
        setLoadingTextIndex((prev) => (prev + 1) % loadingTexts.length)
      }, 2500)
    } else {
      setLoadingTextIndex(0)
    }
    return () => clearInterval(interval)
  }, [step])

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  const handleDeleteAccount = async () => {
    await deleteAccount()
    navigate('/login')
  }

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target
    setForm((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setApiError('')
    setStep('loading')
    setIsSubmitting(true)

    // 데이터 가공
    const birthTime = form.noTime 
      ? null 
      : `${form.birthHour.padStart(2, '0')}:${form.birthMinute.padStart(2, '0')}`

    const payload = {
      name: form.name,
      gender: form.gender,
      calendarType: form.calendarType,
      birthYear: parseInt(form.birthYear),
      birthMonth: parseInt(form.birthMonth),
      birthDay: parseInt(form.birthDay),
      birthTime: birthTime,
      birthPlace: form.birthPlace
    }

    // 최소 5초간 로딩 신비로움을 유지한 후 결과 표시 (UX 가치 극대화)
    const startTime = Date.now()

    try {
      const response = await api.post('/saju/preview', payload)
      const elapsedTime = Date.now() - startTime
      const remainingTime = Math.max(0, 5000 - elapsedTime)

      setTimeout(() => {
        setSajuResult(response.data)
        setLastPayload({ ...payload, imageData: response.data.imageData })
        setIsSaved(false)
        setSaveError('')
        setStep('result')
        setIsSubmitting(false)
      }, remainingTime)

    } catch (err) {
      setStep('input')
      setIsSubmitting(false)
      setApiError(err.response?.data?.message || '사주 분석 요청 중 오류가 발생했습니다.')
    }
  }

  const doSave = async () => {
    if (!lastPayload) return
    setIsSaving(true)
    setSaveError('')
    try {
      const response = await api.post('/saju', lastPayload)
      setSajuResult(response.data)
      setIsSaved(true)
    } catch (err) {
      setSaveError(err.message || '저장 중 오류가 발생했습니다.')
    } finally {
      setIsSaving(false)
    }
  }

  const handleSave = () => {
    if (!user) {
      setAuthError('')
      setAuthMode('login')
      setShowAuthModal(true)
      return
    }
    doSave()
  }

  const handleAuthChange = (e) => {
    const { name, value } = e.target
    setAuthForm((prev) => ({ ...prev, [name]: value }))
  }

  const handleAuthSubmit = async (e) => {
    e.preventDefault()
    setAuthError('')
    setAuthLoading(true)
    try {
      if (authMode === 'login') {
        await login(authForm.email, authForm.password)
      } else {
        await signup(authForm.email, authForm.password, authForm.nickname)
      }
      setShowAuthModal(false)
      setAuthForm({ email: '', password: '', nickname: '' })
      await doSave()
    } catch (err) {
      setAuthError(err.message || '인증 중 오류가 발생했습니다.')
    } finally {
      setAuthLoading(false)
    }
  }

  // 1900 ~ 2030 연도 배열 생성
  const years = Array.from({ length: 131 }, (_, i) => 2030 - i)
  const months = Array.from({ length: 12 }, (_, i) => i + 1)
  const days = Array.from({ length: 31 }, (_, i) => i + 1)
  const hours = Array.from({ length: 24 }, (_, i) => i)
  const minutes = Array.from({ length: 60 }, (_, i) => i)

  const cities = [
    "서울", "경기", "인천", "강원", "충북", "충남", "대전", "세종",
    "전북", "전남", "광주", "경북", "경남", "대구", "울산", "부산", "제주", "해외"
  ]

  // AI가 생성한 운명 분석 텍스트를 【제목】 단위 섹션으로 분리
  const parseDestinyReport = (description) => {
    if (!description) return []
    const sections = []
    const regex = /【([^】]+)】/g
    let match
    let lastTitle = null
    let lastIndex = 0
    while ((match = regex.exec(description)) !== null) {
      if (lastTitle !== null) {
        sections.push({ title: lastTitle, body: description.slice(lastIndex, match.index).trim() })
      }
      lastTitle = match[1]
      lastIndex = regex.lastIndex
    }
    if (lastTitle !== null) {
      sections.push({ title: lastTitle, body: description.slice(lastIndex).trim() })
    } else {
      sections.push({ title: null, body: description.trim() })
    }
    return sections
  }

  return (
    <div className={styles.container}>
      <nav className={styles.nav}>
        <span className={styles.logo}>⚔️ DestinyCode</span>
        <div className={styles.navRight}>
          {user ? (
            <>
              <span className={styles.nickname}>{user.nickname} 님</span>
              <button onClick={handleLogout} className={styles.btnOutline}>
                로그아웃
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className={styles.btnOutline}>로그인</Link>
              <Link to="/signup" className={styles.btnOutline}>회원가입</Link>
            </>
          )}
        </div>
      </nav>

      <main className={styles.main}>
        {/* 1단계: 사주 정보 입력 폼 */}
        {step === 'input' && (
          <div className={styles.inputCard}>
            <h2 className={styles.heading}>✨ 나의 운명 캐릭터 생성</h2>
            <p className={styles.desc}>
              당신의 사주 정보를 만세력 기준으로 입력하세요. <br />
              음양오행의 조화를 분석하여 고유의 RPG 캐릭터를 강림시킵니다.
            </p>

            <form onSubmit={handleSubmit} className={styles.form}>
              {/* 이름 */}
              <div className={styles.formGroup}>
                <label className={styles.label}>이름 (Name)</label>
                <input
                  type="text"
                  name="name"
                  value={form.name}
                  onChange={handleChange}
                  placeholder="모험가의 이름을 알려주세요"
                  className={styles.input}
                  required
                />
              </div>

              {/* 성별 선택 */}
              <div className={styles.formGroup}>
                <label className={styles.label}>성별 (Gender)</label>
                <div className={styles.tabContainer}>
                  <label className={`${styles.tabItem} ${form.gender === 'MALE' ? styles.tabActive : ''}`}>
                    <input
                      type="radio"
                      name="gender"
                      value="MALE"
                      checked={form.gender === 'MALE'}
                      onChange={handleChange}
                      className={styles.radioHidden}
                    />
                    남성 ♂️
                  </label>
                  <label className={`${styles.tabItem} ${form.gender === 'FEMALE' ? styles.tabActive : ''}`}>
                    <input
                      type="radio"
                      name="gender"
                      value="FEMALE"
                      checked={form.gender === 'FEMALE'}
                      onChange={handleChange}
                      className={styles.radioHidden}
                    />
                    여성 ♀️
                  </label>
                </div>
              </div>

              {/* 달력 기준 (음양력) */}
              <div className={styles.formGroup}>
                <label className={styles.label}>달력 기준 (Calendar Type)</label>
                <div className={styles.tabContainer}>
                  <label className={`${styles.tabItem} ${form.calendarType === 'SOLAR' ? styles.tabActive : ''}`}>
                    <input
                      type="radio"
                      name="calendarType"
                      value="SOLAR"
                      checked={form.calendarType === 'SOLAR'}
                      onChange={handleChange}
                      className={styles.radioHidden}
                    />
                    양력 (Solar)
                  </label>
                  <label className={`${styles.tabItem} ${form.calendarType === 'LUNAR_PLAIN' ? styles.tabActive : ''}`}>
                    <input
                      type="radio"
                      name="calendarType"
                      value="LUNAR_PLAIN"
                      checked={form.calendarType === 'LUNAR_PLAIN'}
                      onChange={handleChange}
                      className={styles.radioHidden}
                    />
                    음력 평달
                  </label>
                  <label className={`${styles.tabItem} ${form.calendarType === 'LUNAR_LEAP' ? styles.tabActive : ''}`}>
                    <input
                      type="radio"
                      name="calendarType"
                      value="LUNAR_LEAP"
                      checked={form.calendarType === 'LUNAR_LEAP'}
                      onChange={handleChange}
                      className={styles.radioHidden}
                    />
                    음력 윤달
                  </label>
                </div>
              </div>

              {/* 생년월일 */}
              <div className={styles.formGroup}>
                <label className={styles.label}>생년월일 (Birth Date)</label>
                <div className={styles.inputGrid}>
                  <select name="birthYear" value={form.birthYear} onChange={handleChange} className={styles.select}>
                    {years.map((y) => (
                      <option key={y} value={y}>{y}년</option>
                    ))}
                  </select>
                  <select name="birthMonth" value={form.birthMonth} onChange={handleChange} className={styles.select}>
                    {months.map((m) => (
                      <option key={m} value={m}>{m}월</option>
                    ))}
                  </select>
                  <select name="birthDay" value={form.birthDay} onChange={handleChange} className={styles.select}>
                    {days.map((d) => (
                      <option key={d} value={d}>{d}일</option>
                    ))}
                  </select>
                </div>
              </div>

              {/* 태어난 시간 */}
              <div className={styles.formGroup}>
                <div className={styles.labelWithAction}>
                  <label className={styles.label}>태어난 시간 (Birth Time)</label>
                  <label className={styles.checkboxLabel}>
                    <input
                      type="checkbox"
                      name="noTime"
                      checked={form.noTime}
                      onChange={handleChange}
                      className={styles.checkbox}
                    />
                    시간 모름 ❓
                  </label>
                </div>
                {!form.noTime && (
                  <div className={styles.inputGrid2}>
                    <select name="birthHour" value={form.birthHour} onChange={handleChange} className={styles.select}>
                      {hours.map((h) => (
                        <option key={h} value={h}>{String(h).padStart(2, '0')}시</option>
                      ))}
                    </select>
                    <select name="birthMinute" value={form.birthMinute} onChange={handleChange} className={styles.select}>
                      {minutes.map((m) => (
                        <option key={m} value={m}>{String(m).padStart(2, '0')}분</option>
                      ))}
                    </select>
                  </div>
                )}
              </div>

              {/* 태어난 장소 */}
              <div className={styles.formGroup}>
                <label className={styles.label}>태어난 장소 (Birth Place)</label>
                <select name="birthPlace" value={form.birthPlace} onChange={handleChange} className={styles.selectFull}>
                  {cities.map((city) => (
                    <option key={city} value={city}>{city}</option>
                  ))}
                </select>
              </div>

              {apiError && <p className={styles.errorText}>⚠️ {apiError}</p>}

              <button type="submit" className={styles.btnSubmit}>
                🔮 운명의 캐릭터 강림시키기
              </button>
            </form>
          </div>
        )}

        {/* 3단계: 신비로운 우주적 로딩 화면 */}
        {step === 'loading' && (
          <div className={styles.loadingContainer}>
            <div className={styles.magicPortal}>
              <div className={styles.magicOrb}></div>
              <div className={styles.magicRing}></div>
              <div className={styles.magicRing2}></div>
            </div>
            <div className={styles.loadingTextContainer}>
              <p className={styles.loadingMessage}>{loadingTexts[loadingTextIndex]}</p>
              <div className={styles.progressBar}>
                <div className={styles.progressFill} style={{ width: `${(loadingTextIndex + 1) * 20}%` }}></div>
              </div>
            </div>
          </div>
        )}

        {/* 4단계: 캐릭터 정보 생성 성공 결과 */}
        {step === 'result' && sajuResult && (
          <div className={styles.resultContainer}>
            <div className={styles.resultHeader}>
              <span className={styles.badge}>{sajuResult.characterSummary.element} 속성</span>
              <h2 className={styles.characterTitle}>{sajuResult.characterSummary.title}</h2>
              <h1 className={styles.characterName}>{sajuResult.characterSummary.className} "{sajuResult.name}"</h1>
            </div>

            <div className={styles.cardBody}>
              {/* AI 연성 캐릭터 비주얼 카드 */}
              <div className={styles.characterVisualCard}>
                {sajuResult.imageReady && sajuResult.imageData ? (
                  <div className={styles.aiPortraitFrame}>
                    <img
                      src={sajuResult.imageData}
                      alt="내가 신이라면? AI 캐릭터 일러스트"
                      className={styles.aiPortraitImage}
                    />
                  </div>
                ) : (
                  <div className={styles.aiPortraitFrame}>
                    <p className={styles.imagePlaceholder}>
                      🌫️ 이미지 생성에 실패했습니다.<br />다시 시도해주세요.
                    </p>
                  </div>
                )}
              </div>

              {/* 캐릭터 세부 정보 시트 */}
              <div className={styles.sajuSheet}>
                <h3 className={styles.sectionTitle}>📜 명리(命理) 대운 데이터</h3>
                <div className={styles.sheetGrid}>
                  <div className={styles.sheetItem}>
                    <span className={styles.sheetLabel}>성명</span>
                    <span className={styles.sheetValue}>{sajuResult.name} ({sajuResult.gender === 'MALE' ? '남' : '여'})</span>
                  </div>
                  <div className={styles.sheetItem}>
                    <span className={styles.sheetLabel}>달력</span>
                    <span className={styles.sheetValue}>
                      {sajuResult.calendarType === 'SOLAR' ? '양력' : sajuResult.calendarType === 'LUNAR_PLAIN' ? '음력 평달' : '음력 윤달'}
                    </span>
                  </div>
                  <div className={styles.sheetItem}>
                    <span className={styles.sheetLabel}>생년월일</span>
                    <span className={styles.sheetValue}>{sajuResult.birthYear}년 {sajuResult.birthMonth}월 {sajuResult.birthDay}일</span>
                  </div>
                  <div className={styles.sheetItem}>
                    <span className={styles.sheetLabel}>태어난 시</span>
                    <span className={styles.sheetValue}>{sajuResult.birthTime ? sajuResult.birthTime : '시간 장막(모름)'}</span>
                  </div>
                  <div className={styles.sheetItem}>
                    <span className={styles.sheetLabel}>기운 서린 곳</span>
                    <span className={styles.sheetValue}>{sajuResult.birthPlace}</span>
                  </div>
                </div>
              </div>

              {/* 운명 분석 및 스토리 설명 */}
              <div className={styles.characterStory}>
                <h3 className={styles.sectionTitle}>🛡️ 모험가 운명서 (Destiny Story)</h3>
                {sajuResult.characterSummary.description ? (
                  parseDestinyReport(sajuResult.characterSummary.description).map((section, idx) => (
                    <div key={idx} className={styles.storySection}>
                      {section.title && <h4 className={styles.storySectionTitle}>{section.title}</h4>}
                      <p className={styles.storyText}>{section.body}</p>
                    </div>
                  ))
                ) : (
                  <p className={styles.storyText}>🌫️ 운명 분석에 실패했습니다. 다시 시도해주세요.</p>
                )}
              </div>

            </div>

            {saveError && <p className={styles.errorText}>{saveError}</p>}
            <div className={styles.actionButtons}>
              <button onClick={() => setStep('input')} className={styles.btnOutline}>
                🔄 사주 다시 입력하기
              </button>
              <button
                onClick={handleSave}
                disabled={isSaved || isSaving}
                className={styles.btnPrimary}
              >
                {isSaved ? '✅ 저장됨' : isSaving ? '저장 중...' : '💾 저장하기'}
              </button>
            </div>
          </div>
        )}

        {/* 위험 구역: 회원 탈퇴 (로그인 사용자만) */}
        {step !== 'loading' && user && (
          <div className={styles.danger}>
            <button
              onClick={() => setShowDeleteConfirm(true)}
              className={styles.btnDanger}
            >
              회원 탈퇴
            </button>
          </div>
        )}
      </main>

      {/* 탈퇴 모달 */}
      {showDeleteConfirm && (
        <div className={styles.overlay}>
          <div className={styles.modal}>
            <h3>정말 탈퇴하시겠습니까?</h3>
            <p>탈퇴 시 모든 데이터가 삭제되며 복구할 수 없습니다.</p>
            <div className={styles.modalButtons}>
              <button onClick={() => setShowDeleteConfirm(false)} className={styles.btnOutline}>
                취소
              </button>
              <button onClick={handleDeleteAccount} className={styles.btnDanger}>
                탈퇴하기
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 로그인/회원가입 모달 (저장하기 클릭 시 비로그인 상태면 표시) */}
      {showAuthModal && (
        <div className={styles.overlay}>
          <div className={styles.modal}>
            <h3>{authMode === 'login' ? '로그인하고 저장하기' : '회원가입하고 저장하기'}</h3>
            <p>캐릭터를 저장하려면 로그인이 필요합니다.</p>

            <div className={styles.tabContainer}>
              <button
                type="button"
                className={`${styles.tabItem} ${authMode === 'login' ? styles.tabActive : ''}`}
                onClick={() => { setAuthMode('login'); setAuthError('') }}
              >
                로그인
              </button>
              <button
                type="button"
                className={`${styles.tabItem} ${authMode === 'signup' ? styles.tabActive : ''}`}
                onClick={() => { setAuthMode('signup'); setAuthError('') }}
              >
                회원가입
              </button>
            </div>

            <form onSubmit={handleAuthSubmit} className={styles.form}>
              {authMode === 'signup' && (
                <input
                  type="text"
                  name="nickname"
                  placeholder="닉네임"
                  value={authForm.nickname}
                  onChange={handleAuthChange}
                  className={styles.input}
                  required
                />
              )}
              <input
                type="email"
                name="email"
                placeholder="이메일"
                value={authForm.email}
                onChange={handleAuthChange}
                className={styles.input}
                required
              />
              <input
                type="password"
                name="password"
                placeholder="비밀번호"
                value={authForm.password}
                onChange={handleAuthChange}
                className={styles.input}
                minLength={authMode === 'signup' ? 6 : undefined}
                required
              />

              {authError && <p className={styles.errorText}>⚠️ {authError}</p>}

              <div className={styles.modalButtons}>
                <button type="button" onClick={() => setShowAuthModal(false)} className={styles.btnOutline}>
                  취소
                </button>
                <button type="submit" className={styles.btnPrimary} disabled={authLoading}>
                  {authLoading ? '처리 중...' : (authMode === 'login' ? '로그인 후 저장' : '가입 후 저장')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
