import { forwardRef, type InputHTMLAttributes } from 'react';

export type FormInputProps = InputHTMLAttributes<HTMLInputElement>;

const FormInput = forwardRef<HTMLInputElement, FormInputProps>(function FormInput(props, ref) {
  const {
    id,
    name,
    type = 'text',
    value,
    onChange,
    onBlur,
    className = 'market-input',
    defaultValue: _unusedDefaultValue,
    ...rest
  } = props;
  void _unusedDefaultValue;

  return (
    <input
      {...rest}
      id={id}
      name={name}
      type={type}
      value={value}
      onChange={onChange}
      onBlur={onBlur}
      ref={ref}
      className={className}
    />
  );
});

export default FormInput;
