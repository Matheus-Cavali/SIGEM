import Icon from './Icon'

export default function PageHeader({ title, subtitle, actionLabel, actionIcon = 'plus', onAction }) {
  return (
    <header className="page-header">
      <div>
        <h1>{title}</h1>
        <p>{subtitle}</p>
      </div>
      {actionLabel && (
        <button className="primary-action" onClick={onAction}>
          <Icon name={actionIcon} size={16} />
          <span>{actionLabel}</span>
        </button>
      )}
    </header>
  )
}
