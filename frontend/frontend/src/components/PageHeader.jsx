import Icon from './Icon'

export default function PageHeader({ title, subtitle, actionLabel, onAction }) {
  return (
    <header className="page-header">
      <div>
        <h1>{title}</h1>
        <p>{subtitle}</p>
      </div>
      {actionLabel && (
        <button className="primary-action" onClick={onAction}>
          <Icon name="plus" size={16} />
          <span>{actionLabel}</span>
        </button>
      )}
    </header>
  )
}
